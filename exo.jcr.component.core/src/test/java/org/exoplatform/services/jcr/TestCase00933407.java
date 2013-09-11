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
package org.exoplatform.services.jcr;


import java.io.IOException;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.AccessControlException;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.impl.core.value.PermissionValue;
import org.exoplatform.services.jcr.core.CredentialsImpl;
import org.exoplatform.services.jcr.impl.core.value.StringValue;
import org.exoplatform.services.security.IdentityConstants;

/**
 * Created by The eXo Platform SAS.
 * 
 * NOTE: Make sure you have the files pointed below!
 */

public class TestCase00933407 extends JcrAPIBaseTest
{
   
   private Node testFolder = null;

   public void setUp() throws Exception
   {
      super.setUp();// .repository
      testFolder = root.addNode("case00933407", "nt:folder");
      session.save();
   }

   public void testAddImage() throws Exception
   {
      String nodeName = "noacl";
      
      // add a node
      Node fileNode = testFolder.addNode(nodeName, "nt:file"); 

      // add content
      Node contentNode = fileNode.addNode("jcr:content", "nt:resource");
      contentNode.setProperty("jcr:encoding", "UTF-8");
      contentNode.setProperty("jcr:data", TestCase00933407.class.getResourceAsStream("/test_tiff_file.tiff"));
      contentNode.setProperty("jcr:mimeType", new StringValue("image/tiff"));
      contentNode.setProperty("jcr:lastModified", Calendar.getInstance());

      session.save();
      
      // check if data exists
      assertNotNull(testFolder.getNode(nodeName).getNode("jcr:content").getProperty("jcr:data").getValue());
      
      // check permissions
      Session session1 = repository.login(new CredentialsImpl(IdentityConstants.ANONIM, "".toCharArray()));
      try {
        session1.checkPermission("/case00933407/" + nodeName, PermissionType.READ);
        session1.checkPermission("/case00933407/" + nodeName + "/jcr:content", PermissionType.READ);   
      }
      catch (AccessControlException e)
      {
        fail("Permission error " + e.getMessage());
      }           
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
      contentNode.setProperty("jcr:encoding", "UTF-8");
      contentNode.setProperty("jcr:data", TestCase00933407.class.getResourceAsStream("/test_tiff_file.tiff"));
      contentNode.setProperty("jcr:mimeType", new StringValue("image/tiff"));
      contentNode.setProperty("jcr:lastModified", Calendar.getInstance());

      session.save();
      
      // check if data exists
      assertNotNull(testFolder.getNode(nodeName).getNode("jcr:content").getProperty("jcr:data").getValue());
      
      // check permissions
      Session session1 = repository.login(new CredentialsImpl(IdentityConstants.ANONIM, "".toCharArray()));
      try {
        session1.checkPermission("/case00933407/" + nodeName, PermissionType.READ);
        session1.checkPermission("/case00933407/" + nodeName + "/jcr:content", PermissionType.READ);   
      }
      catch (AccessControlException e)
      {
        fail("Permission error " + e.getMessage());
      }           
   }
   
   public void testAddImageWithAclOnNodeAndContent() throws Exception
   {
      String nodeName = "aclOnNodeAndContent";
      
      // add a node
      Node fileNode = testFolder.addNode(nodeName, "nt:file"); 
      // set permissions on fileNode
      fileNode.addMixin("exo:privilegeable");
      fileNode.setProperty("exo:permissions", getDefaultPermission());
      
      // add content
      Node contentNode = fileNode.addNode("jcr:content", "nt:resource");
      contentNode.setProperty("jcr:encoding", "UTF-8");
      contentNode.setProperty("jcr:data", TestCase00933407.class.getResourceAsStream("/test_tiff_file.tiff"));
      contentNode.setProperty("jcr:mimeType", new StringValue("image/tiff"));
      contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
      // set permissions on contentNode
      contentNode.addMixin("exo:privilegeable");
      contentNode.setProperty("exo:permissions", getDefaultPermission());

      session.save();
      
      // check if data exists
      assertNotNull(testFolder.getNode(nodeName).getNode("jcr:content").getProperty("jcr:data").getValue());
      
      // check permissions
      Session session1 = repository.login(new CredentialsImpl(IdentityConstants.ANONIM, "".toCharArray()));
      try {
        session1.checkPermission("/case00933407/" + nodeName, PermissionType.READ);
        session1.checkPermission("/case00933407/" + nodeName + "/jcr:content", PermissionType.READ);   
      }
      catch (AccessControlException e)
      {
        fail("Permission error " + e.getMessage());
      }           
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
   

   protected void tearDown() throws Exception
   {
      testFolder.remove();
      root.save();
      super.tearDown();
   }
}
