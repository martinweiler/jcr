/*
 * Copyright (C) 2009 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.services.jcr.impl.storage.value.fs.operations;

import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.ByteArrayPersistedValueData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.FilePersistedValueData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.StreamPersistedValueData;
import org.exoplatform.services.jcr.impl.util.io.PrivilegedFileHelper;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>
 * Date: 03.04.2009
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: ValueFileIOHelper.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class ValueFileIOHelper
{

   /**
    * I/O buffer size for internal VS operations (32K).
    */
   public static final int IOBUFFER_SIZE = 32 * 1024; // 32K

   /**
    * Helper logger.
    */
   protected static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.ValueFileIOHelper");

   /**
    * Read value from file.
    * 
    * @param file
    *          File
    * @param orderNum
    *          - used in PersistedValueData logic
    * @param maxBufferSize
    *          - threshold for spooling
    * @return ValueData
    * @throws IOException
    *           if error
    */
   protected ValueData readValue(final File file, final int orderNum, final int maxBufferSize) throws IOException
   {
      PrivilegedExceptionAction<Object> action = new PrivilegedExceptionAction<Object>()
      {
         public Object run() throws Exception
         {
            long fileSize = PrivilegedFileHelper.length(file);

            if (fileSize > maxBufferSize)
            {
               return new FilePersistedValueData(orderNum, file);
            }
            else
            {
               FileInputStream is = PrivilegedFileHelper.fileInputStream(file);
               try
               {
                  int buffSize = (int)fileSize;
                  byte[] res = new byte[buffSize];
                  int rpos = 0;
                  int r = -1;
                  byte[] buff = new byte[IOBUFFER_SIZE > buffSize ? IOBUFFER_SIZE : buffSize];
                  while ((r = is.read(buff)) >= 0)
                  {
                     System.arraycopy(buff, 0, res, rpos, r);
                     rpos += r;
                  }
                  return new ByteArrayPersistedValueData(orderNum, res);
               }
               finally
               {
                  is.close();
               }
            }
         }
      };
      try
      {
         return (ValueData)AccessController.doPrivileged(action);
      }
      catch (PrivilegedActionException pae)
      {
         Throwable cause = pae.getCause();
         if (cause instanceof IOException)
         {
            throw (IOException)cause;
         }
         else if (cause instanceof RuntimeException)
         {
            throw (RuntimeException)cause;
         }
         else
         {
            throw new RuntimeException(cause);
         }
      }
   }

   /**
    * Write value to a file.
    * 
    * @param file
    *          File
    * @param value
    *          ValueData
    * @throws IOException
    *           if error occurs
    */
   protected void writeValue(File file, ValueData value) throws IOException
   {
      if (value.isByteArray())
      {
         writeByteArrayValue(file, value);
      }
      else
      {
         writeStreamedValue(file, value);
      }
   }

   /**
    * Write value array of bytes to a file.
    * 
    * @param file
    *          File
    * @param value
    *          ValueData
    * @throws IOException
    *           if error occurs
    */
   protected void writeByteArrayValue(final File file, final ValueData value) throws IOException
   {
      PrivilegedExceptionAction<Object> action = new PrivilegedExceptionAction<Object>()
      {
         public Object run() throws Exception
         {
            OutputStream out = PrivilegedFileHelper.fileOutputStream(file);
            try
            {
               out.write(value.getAsByteArray());
            }
            finally
            {
               out.close();
            }

            return null;
         }
      };
      try
      {
         AccessController.doPrivileged(action);
      }
      catch (PrivilegedActionException pae)
      {
         Throwable cause = pae.getCause();
         if (cause instanceof IOException)
         {
            throw (IOException)cause;
         }
         else if (cause instanceof RuntimeException)
         {
            throw (RuntimeException)cause;
         }
         else
         {
            throw new RuntimeException(cause);
         }
      }
   }

   /**
    * Write streamed value to a file.
    * 
    * @param file
    *          File
    * @param value
    *          ValueData
    * @throws IOException
    *           if error occurs
    */
   protected void writeStreamedValue(final File file, final ValueData value) throws IOException
   {
      PrivilegedExceptionAction<Object> action = new PrivilegedExceptionAction<Object>()
      {
         public Object run() throws Exception
         {
            // do what you want
            // stream Value
            if (value instanceof StreamPersistedValueData)
            {
               StreamPersistedValueData streamed = (StreamPersistedValueData)value;

               if (streamed.isPersisted())
               {
                  // already persisted in another Value, copy it to this Value
                  copyClose(streamed.getAsStream(), PrivilegedFileHelper.fileOutputStream(file));
               }
               else
               {
                  // the Value not yet persisted, i.e. or in client stream or spooled to a temp file
                  File tempFile;
                  if ((tempFile = streamed.getTempFile()) != null)
                  {
                     // it's spooled Value, try move its file to VS
                     if (!PrivilegedFileHelper.renameTo(tempFile, file))
                     {
                        // not succeeded - copy bytes, temp file will be deleted by transient ValueData
                        if (LOG.isDebugEnabled())
                        {
                           LOG
                              .debug("Value spool file move (rename) to Values Storage is not succeeded. Trying bytes copy. Spool file: "
                                 + PrivilegedFileHelper.getAbsolutePath(tempFile)
                                 + ". Destination: "
                                 + PrivilegedFileHelper.getAbsolutePath(file));
                        }

                        copyClose(PrivilegedFileHelper.fileInputStream(tempFile), PrivilegedFileHelper
                           .fileOutputStream(file));
                     }
                  }
                  else
                  {
                     // not spooled, use client InputStream
                     copyClose(streamed.getStream(), PrivilegedFileHelper.fileOutputStream(file));
                  }

                  // link this Value to file in VS
                  streamed.setPersistedFile(file);
               }
            }
            else
            {
               // copy from Value stream to the file, e.g. from FilePersistedValueData to this Value
               copyClose(value.getAsStream(), PrivilegedFileHelper.fileOutputStream(file));
            }

            return null;
         }
      };
      try
      {
         AccessController.doPrivileged(action);
      }
      catch (PrivilegedActionException pae)
      {
         Throwable cause = pae.getCause();
         if (cause instanceof IOException)
         {
            throw (IOException)cause;
         }
         else if (cause instanceof RuntimeException)
         {
            throw (RuntimeException)cause;
         }
         else
         {
            throw new RuntimeException(cause);
         }
      }
   }

   /**
    * Stream value data to the output.
    * 
    * @param out
    *          OutputStream
    * @param value
    *          ValueData
    * @throws IOException
    *           if error occurs
    */
   protected void writeOutput(OutputStream out, ValueData value) throws IOException
   {
      if (value.isByteArray())
      {
         byte[] buff = value.getAsByteArray();
         out.write(buff);
      }
      else
      {
         InputStream in;
         if (value instanceof StreamPersistedValueData)
         {

            StreamPersistedValueData streamed = (StreamPersistedValueData)value;
            if (streamed.isPersisted())
            {
               // already persisted in another Value, copy it to this Value
               in = streamed.getAsStream();
            }
            else
            {
               in = streamed.getStream();
               if (in == null)
               {
                  in = PrivilegedFileHelper.fileInputStream(streamed.getTempFile());
               }
            }
         }
         else
         {
            in = value.getAsStream();
         }

         try
         {
            copy(in, out);
         }
         finally
         {
            in.close();
         }
      }
   }

   /**
    * Copy input to output data using NIO.
    * 
    * @param in
    *          InputStream
    * @param out
    *          OutputStream
    * @return The number of bytes, possibly zero, that were actually copied
    * @throws IOException
    *           if error occurs
    */
   protected long copy(InputStream in, OutputStream out) throws IOException
   {
      // compare classes as in Java6 Channels.newChannel(), Java5 has a bug in newChannel().
      boolean inFile = in instanceof FileInputStream && FileInputStream.class.equals(in.getClass());
      boolean outFile = out instanceof FileOutputStream && FileOutputStream.class.equals(out.getClass());
      if (inFile && outFile)
      {
         // it's user file
         FileChannel infch = ((FileInputStream)in).getChannel();
         FileChannel outfch = ((FileOutputStream)out).getChannel();

         long size = 0;
         long r = 0;
         do
         {
            r = outfch.transferFrom(infch, r, infch.size());
            size += r;
         }
         while (r < infch.size());
         return size;
      }
      else
      {
         // it's user stream (not a file)
         ReadableByteChannel inch = inFile ? ((FileInputStream)in).getChannel() : Channels.newChannel(in);
         WritableByteChannel outch = outFile ? ((FileOutputStream)out).getChannel() : Channels.newChannel(out);

         // TODO buffers show same perfomance as bytes copy via Input/Output streams
         // NIO buffers article http://www.odi.ch/weblog/posting.php?posting=371
         long size = 0;
         int r = 0;
         ByteBuffer buff = ByteBuffer.allocate(IOBUFFER_SIZE);
         buff.clear();
         while ((r = inch.read(buff)) >= 0)
         {
            buff.flip();

            // copy all
            do
            {
               outch.write(buff);
            }
            while (buff.hasRemaining());

            buff.clear();
            size += r;
         }

         if (outFile)
            ((FileChannel)outch).force(true); // force all data to FS

         return size;
      }
   }

   /**
    * Copy input to output data using NIO. Input and output streams will be closed after the
    * operation.
    * 
    * @param in
    *          InputStream
    * @param out
    *          OutputStream
    * @return The number of bytes, possibly zero, that were actually copied
    * @throws IOException
    *           if error occurs
    */
   protected long copyClose(InputStream in, OutputStream out) throws IOException
   {
      try
      {
         try
         {
            return copy(in, out);
         }
         finally
         {
            in.close();
         }
      }
      finally
      {
         out.close();
      }
   }
}
