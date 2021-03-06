/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.exoplatform.services.jcr.api.core.query.lucene.directory;

import junit.framework.TestCase;

import org.apache.lucene.store.Directory;
import org.exoplatform.services.jcr.impl.core.query.lucene.SearchIndex;
import org.exoplatform.services.jcr.impl.core.query.lucene.directory.DirectoryManager;
import org.exoplatform.services.jcr.impl.core.query.lucene.directory.FSDirectoryManager;
import org.exoplatform.services.jcr.impl.core.query.lucene.directory.RAMDirectoryManager;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/**
 * <code>DirectoryManagerTest</code> performs tests on directory manager
 * implementations.
 */
public class DirectoryManagerTest extends TestCase
{

   private static final Collection IMPLEMENTATIONS =
      Arrays.asList(new Class[]{FSDirectoryManager.class, RAMDirectoryManager.class});

   private static final SearchIndex INDEX = new SearchIndex();

   private static final String TEST = "test";

   private static final String RENAMED = "renamed";

   static
   {
      File dst = new File(new File("target"), "directory-factory-test");
      dst.mkdirs();
      INDEX.setPath(dst.getAbsolutePath());
   }

   @Override
   protected void tearDown() throws Exception
   {
//      new File(INDEX.getPath(), TEST).delete();
//      new File(INDEX.getPath(), RENAMED).delete();
   }

   public void testHasDirectory() throws Exception
   {
      execute(new Callable()
      {
         public void call(DirectoryManager directoryManager) throws Exception
         {
            Directory dir = directoryManager.getDirectory(TEST);
            assertTrue("Impl:" + directoryManager.getClass().getName(), directoryManager.hasDirectory(TEST));
            dir.close();
         }
      });
   }

   public void testDelete() throws Exception
   {
      execute(new Callable()
      {
         public void call(DirectoryManager directoryManager) throws Exception
         {
            directoryManager.getDirectory(TEST).close();
            directoryManager.delete(TEST);
            assertFalse("Impl:" + directoryManager.getClass().getName(), directoryManager.hasDirectory(TEST));
         }
      });
   }

   public void testGetDirectoryNames() throws Exception
   {
      execute(new Callable()
      {
         public void call(DirectoryManager directoryManager) throws Exception
         {
            directoryManager.getDirectory(TEST).close();
            assertTrue("Impl:" + directoryManager.getClass().getName(), Arrays.asList(directoryManager.getDirectoryNames()).contains(TEST));
         }
      });
   }

   public void testRename() throws Exception
   {
      execute(new Callable()
      {
         public void call(DirectoryManager directoryManager) throws Exception
         {
            directoryManager.getDirectory(TEST).close();
            directoryManager.rename(TEST, RENAMED);
            assertTrue("Impl:" + directoryManager.getClass().getName(), directoryManager.hasDirectory(RENAMED));
            assertFalse("Impl:" + directoryManager.getClass().getName(), directoryManager.hasDirectory(TEST));
         }
      });
   }

   private void execute(Callable callable) throws Exception
   {
      for (Iterator it = IMPLEMENTATIONS.iterator(); it.hasNext();)
      {
         Class clazz = (Class)it.next();
         DirectoryManager dirMgr = (DirectoryManager)clazz.newInstance();
         dirMgr.init(INDEX);
         try
         {
            callable.call(dirMgr);
         }
         finally
         {
            dirMgr.dispose();
         }
      }
   }

   private interface Callable
   {

      public void call(DirectoryManager directoryManager) throws Exception;
   }
}
