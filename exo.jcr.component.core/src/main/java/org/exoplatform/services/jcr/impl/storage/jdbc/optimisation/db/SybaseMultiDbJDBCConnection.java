/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.impl.storage.jdbc.optimisation.db;

import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig;
import org.exoplatform.services.jcr.util.IdGenerator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: SybaseMultiDbJDBCConnection.java 111 4.05.2011 serg $
 */
public class SybaseMultiDbJDBCConnection extends MultiDbJDBCConnection
{

   /**
    * SELECT_LIMIT_OFFSET_NODES_INTO_TEMPORARY_TABLE
    */
   protected String SELECT_LIMIT_OFFSET_NODES_INTO_TEMPORARY_TABLE;

   /**
    * DELETE_TEMPORARY_TABLE_A
    */
   protected String DELETE_TEMPORARY_TABLE_A;

   protected PreparedStatement selectLimitOffsetNodesIntoTemporaryTable;

   protected PreparedStatement deleteTemporaryTableA;

   /**
    * @param dbConnection
    *          JDBC connection, should be opened before
    * @param readOnly
    *          boolean if true the dbConnection was marked as READ-ONLY.
    * @param containerConfig
    *          Workspace Storage Container configuration
    */
   public SybaseMultiDbJDBCConnection(Connection dbConnection, boolean readOnly, JDBCDataContainerConfig containerConfig)
      throws SQLException
   {
      super(dbConnection, readOnly, containerConfig);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected String escapeSpecialChars(String pattern)
   {
      char[] chars = pattern.toCharArray();
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < chars.length; i++)
      {
         switch (chars[i])
         {
            case '*' :
               sb.append('%');
               break;
            case '_' :
            case '%' :
            case '[' : //Sybase pattern special symbol
            case ']' : //Sybase pattern special symbol
               sb.append(this.getWildcardEscapeSymbol());
            default :
               sb.append(chars[i]);
         }
      }
      return sb.toString();
   }

   /**
    * {@inheritDoc}
    */
   protected void prepareQueries() throws SQLException
   {

      super.prepareQueries();

      FIND_PROPERTY_BY_ID =
         "select datalength(DATA), I.P_TYPE, V.STORAGE_DESC from " + JCR_ITEM + " I, " + JCR_VALUE
            + " V where I.ID = ? and V.PROPERTY_ID = I.ID";

      SELECT_LIMIT_OFFSET_NODES_INTO_TEMPORARY_TABLE =
         "select TOP ${TOP} I.ID, I.PARENT_ID, I.NAME, I.VERSION, I.I_INDEX, I.N_ORDER_NUM into "
            + SybaseJDBCConnectionHelper.TEMP_A_TABLE_NAME + " from " + JCR_ITEM
            + " I (index "+JCR_PK_ITEM+") where I.I_CLASS=1 AND I.ID > ? order by I.ID";

      FIND_NODES_AND_PROPERTIES =
         "select " + SybaseJDBCConnectionHelper.TEMP_A_TABLE_NAME
            + ".*, P.ID AS P_ID, P.NAME AS P_NAME, P.VERSION AS P_VERSION, P.P_TYPE, P.P_MULTIVALUED,"
            + " V.DATA, V.ORDER_NUM, V.STORAGE_DESC from " + JCR_VALUE + " V, " + JCR_ITEM + " P, "
            + SybaseJDBCConnectionHelper.TEMP_A_TABLE_NAME + " where P.PARENT_ID = "
            + SybaseJDBCConnectionHelper.TEMP_A_TABLE_NAME + ".ID and P.I_CLASS=2 and V.PROPERTY_ID=P.ID "
            + "order by " + SybaseJDBCConnectionHelper.TEMP_A_TABLE_NAME + ".ID";

      DELETE_TEMPORARY_TABLE_A = "drop table " + SybaseJDBCConnectionHelper.TEMP_A_TABLE_NAME;

      FIND_WORKSPACE_DATA_SIZE = "select sum(datalength(DATA)) from " + JCR_VALUE;

      FIND_NODE_DATA_SIZE =
         "select sum(datalength(DATA)) from " + JCR_ITEM + " I, " + JCR_VALUE
            + " V  where I.PARENT_ID=? and I.I_CLASS=2 and I.ID=V.PROPERTY_ID";

      FIND_VALUE_STORAGE_DESC_AND_SIZE = "select datalength(DATA), STORAGE_DESC from " + JCR_VALUE + " where PROPERTY_ID=?";
   }

   /**
    * {@inheritDoc}
    */
   protected ResultSet findNodesAndProperties(String lastNodeId, int offset, int limit) throws SQLException
   {
      String tempTableAName = "tempdb..a" + IdGenerator.generate();

      boolean tempTableACreated = false;

      try
      {
         // the Sybase is not allowed DDL query (CREATE TABLE, DROP TABLE, etc. ) within a multi-statement transaction
         if (!dbConnection.getAutoCommit())
         {
            dbConnection.setAutoCommit(true);
         }

         selectLimitOffsetNodesIntoTemporaryTable =
            dbConnection.prepareStatement(SELECT_LIMIT_OFFSET_NODES_INTO_TEMPORARY_TABLE.replaceAll(
               SybaseJDBCConnectionHelper.TEMP_A_TABLE_NAME, tempTableAName).replace("${TOP}",
                  new Integer(offset + limit).toString()));

         if (findNodesAndProperties != null)
         {
            findNodesAndProperties.close();
         }

         findNodesAndProperties =
            dbConnection.prepareStatement(FIND_NODES_AND_PROPERTIES.replaceAll(
               SybaseJDBCConnectionHelper.TEMP_A_TABLE_NAME, tempTableAName));

         deleteTemporaryTableA =
            dbConnection.prepareStatement(DELETE_TEMPORARY_TABLE_A.replaceAll(
               SybaseJDBCConnectionHelper.TEMP_A_TABLE_NAME, tempTableAName));

         selectLimitOffsetNodesIntoTemporaryTable.setString(1, lastNodeId);
         selectLimitOffsetNodesIntoTemporaryTable.execute();

         tempTableACreated = true;

         return findNodesAndProperties.executeQuery();
      }
      finally
      {
         if (tempTableACreated)
         {
            try
            {
               deleteTemporaryTableA.execute();
            }
            catch (SQLException e)
            {
               LOG.warn("Can not delete temporary table " + tempTableAName);
            }
         }

         // close prepared statement since we always create new
         if (selectLimitOffsetNodesIntoTemporaryTable != null)
         {
            selectLimitOffsetNodesIntoTemporaryTable.close();
         }

         if (deleteTemporaryTableA != null)
         {
            deleteTemporaryTableA.close();
         }
      }
   }
}