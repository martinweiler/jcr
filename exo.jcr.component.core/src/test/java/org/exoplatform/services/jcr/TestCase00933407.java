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
import java.util.HashSet;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.Value;

import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.core.CredentialsImpl;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.core.value.PermissionValue;
import org.exoplatform.services.jcr.impl.core.value.StringValue;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;


/**
 * Created by The eXo Platform SAS.
 * 
 * NOTE: Make sure you have the files pointed below!
 */

public class TestCase00933407 extends JcrAPIBaseTest
{
   protected static final Log LOG = ExoLogger.getLogger("exo.jcr.test.Case00933407");
   private static final String JCR_PATH = "/" + TestCase00933407.class.getName() + "/";

   public void setUp() throws Exception
   {
      super.setUp();// .repository
   }

   
   public void testAddImageWithAclOnNode() throws Exception
   {
      String nodeName = "aclOnNode";

      CredentialsImpl rootCred = new CredentialsImpl("root", "exo".toCharArray());
      CredentialsImpl anonimCred = new CredentialsImpl(IdentityConstants.ANONIM, "".toCharArray());
      
      // 1. create base folder
      Session rootSession = (SessionImpl)repository.login(rootCred, "ws");
      try {
          Node rootNode = rootSession.getRootNode();
          Node folderNode = rootNode.addNode(TestCase00933407.class.getName(), "nt:unstructured");   
          // set permissions on folderNode
          folderNode.addMixin("exo:privilegeable");
          folderNode.setProperty("exo:permissions", getDefaultPermission());
          LOG.info("created folderNode with permissions");
          rootSession.save();   
      } catch(Exception ex) {
        fail("ERROR " + ex.getMessage());
      } finally {
        if(rootSession!=null) {
            rootSession.logout();
        }
      }
      
      // 2. add a node
      Session rootSession2 = (SessionImpl)repository.login(rootCred, "ws");      
      try {
          Item parentItem = rootSession2.getItem(JCR_PATH);
          Node parentNode = null;
          if(parentItem!=null && parentItem.isNode()) {
            parentNode = (Node) parentItem;
          }
          Node fileNode = parentNode.addNode(nodeName, "nt:file"); 
          // set permissions on fileNode
          fileNode.addMixin("exo:privilegeable");
          fileNode.setProperty("exo:permissions", getDefaultPermission());
          
          // add content
          Node contentNode = fileNode.addNode("jcr:content", "nt:resource");
          contentNode.setProperty("jcr:encoding", "UTF-8");
          contentNode.setProperty("jcr:data", TestCase00933407.class.getResourceAsStream("/test_tiff_file.tiff"));
          contentNode.setProperty("jcr:mimeType", new StringValue("image/tiff"));
          contentNode.setProperty("jcr:lastModified", Calendar.getInstance());

          rootSession2.save();
          // check if data exists
          assertNotNull(parentNode.getNode(nodeName).getNode("jcr:content").getProperty("jcr:data").getValue());
      } catch(Exception ex) {
        fail("ERROR " + ex.getMessage());
      } finally {
        if(rootSession2!=null) {
            rootSession2.logout();
        }
      }
      
      
      // 3. check permissions
      //Session anonimSession = (SessionImpl)repository.login(anonimCred);
      Identity id = new Identity(IdentityConstants.ANONIM, new HashSet<MembershipEntry>());
      ConversationState.setCurrent(new ConversationState(id));
      SessionProvider anonimProvider = SessionProvider.createAnonimProvider();			
      Session anonimSession = anonimProvider.getSession("ws", repository);      
      try {
        Node node = (Node)anonimSession.getItem(JCR_PATH + nodeName);
        assertNotNull(node);
      } catch(Exception ex) {
        fail("ERROR " + ex.getMessage());
      } finally {
        if(anonimSession!=null) {
            anonimSession.logout();
        }
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
      super.tearDown();
   }
}
