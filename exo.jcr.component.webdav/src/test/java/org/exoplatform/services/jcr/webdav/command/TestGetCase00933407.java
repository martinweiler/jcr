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
package org.exoplatform.services.jcr.webdav.command;

import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.CredentialsImpl;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.impl.core.value.PermissionValue;
import org.exoplatform.services.jcr.impl.core.value.StringValue;
import org.exoplatform.services.jcr.impl.core.version.VersionImpl;
import org.exoplatform.services.jcr.webdav.BaseStandaloneTest;
import org.exoplatform.services.jcr.webdav.WebDavConst;
import org.exoplatform.services.jcr.webdav.WebDavConstants.WebDAVMethods;
import org.exoplatform.services.jcr.webdav.utils.TestUtils;
import org.exoplatform.services.rest.ExtHttpHeaders;
import org.exoplatform.services.rest.ext.provider.XSLTStreamingOutput;
import org.exoplatform.services.rest.impl.ContainerResponse;
import org.exoplatform.services.rest.impl.MultivaluedMapImpl;
import org.exoplatform.services.security.IdentityConstants;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Created by The eXo Platform SAS Author : Dmytro Katayev
 * work.visor.ck@gmail.com Aug 13, 2008
 */
public class TestGetCase00933407 extends BaseStandaloneTest
{
   
   
   private Node testFolder = null;
   
   private String imageType = "image/tiff";
   private String charSet = "UTF-8";
      
   public void setUp() throws Exception
   {
      super.setUp();// .repository
      testFolder = root.addNode("case00933407", "nt:folder");
      session.save();
   }   

   @Override
   protected String getRepositoryName()
   {
      return null;
   }
   

   protected void tearDown() throws Exception
   {
      testFolder.remove();
      root.save();
      super.tearDown();
   }   
   
   
   public void testAddImageWithAclOnNode() throws Exception
   {
      String nodeName = "aclOnNode";
      
      // add a node
      Node fileNode = testFolder.addNode(nodeName, "nt:file"); 
      // set permissions on fileNode
      fileNode.addMixin("exo:privilegeable");
      fileNode.setProperty("exo:permissions", getDefaultPermission());
      
      // add content
      Node contentNode = fileNode.addNode("jcr:content", "nt:resource");
      contentNode.setProperty("jcr:encoding", charSet);
      contentNode.setProperty("jcr:data", TestGetCase00933407.class.getResourceAsStream("/test_tiff_file.tiff"));
      contentNode.setProperty("jcr:mimeType", new StringValue(imageType));
      contentNode.setProperty("jcr:lastModified", Calendar.getInstance());

      session.save();

      // check if data exists
      assertNotNull(testFolder.getNode(nodeName).getNode("jcr:content").getProperty("jcr:data").getValue());
      
      // Login as anonim. 
      // Tthis session use on server side, thank SessionProvider.
      repository.login(new CredentialsImpl(IdentityConstants.ANONIM, "exo".toCharArray()), "ws");

      String path = getPathWS() + "/webdav-test/case00933407/aclOnNode";

      ContainerResponse response = service(WebDAVMethods.GET, path, "", null, null);
      assertEquals("Successful result expected (200), but actual is: " + response.getStatus(), 200, response.getStatus());
      
      //System.out.println(" *** contentType= " + response.getContentType() + ", getEntityType=" + response.getEntityType()); 

          
   }   
   
   
   private PermissionValue[] getDefaultPermission() throws IOException
   {
        PermissionValue[] permission = { new PermissionValue(IdentityConstants.ANONIM, PermissionType.READ),
					new PermissionValue(IdentityConstants.ANY, PermissionType.READ),
					new PermissionValue("*:/platform/administrators", PermissionType.READ),
					new PermissionValue("*:/platform/administrators", PermissionType.ADD_NODE),
					new PermissionValue("*:/platform/administrators", PermissionType.SET_PROPERTY),
					new PermissionValue("*:/platform/administrators", PermissionType.REMOVE) }; 
	    return permission;  
   }   
   
}
