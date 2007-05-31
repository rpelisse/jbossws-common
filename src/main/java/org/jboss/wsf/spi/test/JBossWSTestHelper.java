/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.wsf.spi.test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.jboss.logging.Logger;
import org.jboss.util.NotImplementedException;
import org.jboss.wsf.spi.utils.ObjectNameFactory;

/**
 * A JBossWS test helper that deals with test deployment/undeployment, etc.
 *
 * @author Thomas.Diesler@jboss.org
 * @since 14-Oct-2004
 */
public class JBossWSTestHelper
{
   // provide logging
   private static Logger log = Logger.getLogger(JBossWSTestHelper.class);

   private static MBeanServerConnection server;
   private static String integrationTarget;

   /** Deploy the given archive
    */
   public void deploy(String archive) throws Exception
   {
      URL url = getArchiveURL(archive);
      getDeployer().deploy(url);
   }

   /** Undeploy the given archive
    */
   public void undeploy(String archive) throws Exception
   {
      URL url = getArchiveURL(archive);
      getDeployer().undeploy(url);
   }

   /** True, if -Djbossws.integration.target=jboss50 */
   public static boolean isTargetJBoss50()
   {
      String target = getIntegrationTarget();
      return "jboss50".equals(target);
   }

   /** True, if -Djbossws.integration.target=jboss42 */
   public static boolean isTargetJBoss42()
   {
      String target = getIntegrationTarget();
      return "jboss42".equals(target);
   }

   public boolean isIntegrationNative()
   {
      String vendor = Service.class.getPackage().getImplementationVendor();
      return vendor.startsWith("JBoss");
   }
   
   public boolean isIntegrationSunRI()
   {
      String vendor = Service.class.getPackage().getImplementationVendor();
      return vendor.startsWith("Sun Microsystems");
   }
   
   public boolean isIntegrationXFire()
   {
      throw new NotImplementedException();
   }
   
   /**
    * Get the JBoss server host from system property "jboss.bind.address"
    * This defaults to "localhost"
    */
   public static String getServerHost()
   {
      String hostName = System.getProperty("jboss.bind.address", "localhost");
      return hostName;
   }

   public static MBeanServerConnection getServer()
   {
      if (server == null)
      {
         Hashtable jndiEnv = null;
         try
         {
            InitialContext iniCtx = new InitialContext();
            jndiEnv = iniCtx.getEnvironment();
            server = (MBeanServerConnection)iniCtx.lookup("jmx/invoker/RMIAdaptor");
         }
         catch (NamingException ex)
         {
            throw new RuntimeException("Cannot obtain MBeanServerConnection using jndi props: " + jndiEnv, ex);
         }
      }
      return server;
   }

   private TestDeployer getDeployer()
   {
      return new TestDeployerJBoss(getServer());
   }

   private static String getIntegrationTarget()
   {
      if (integrationTarget == null)
      {
         integrationTarget = System.getProperty("jbossws.integration.target");

         if (integrationTarget == null)
            throw new IllegalStateException("Cannot obtain jbossws.integration.target");

         // Read the JBoss SpecificationVersion
         try
         {
            ObjectName oname = ObjectNameFactory.create("jboss.system:type=ServerConfig");
            String jbossVersion = (String)getServer().getAttribute(oname, "SpecificationVersion");
            if (jbossVersion.startsWith("5.0"))
               jbossVersion = "jboss50";
            else if (jbossVersion.startsWith("4.2"))
               jbossVersion = "jboss42";
            else throw new RuntimeException("Unsupported jboss version: " + jbossVersion);

            if (jbossVersion.equals(integrationTarget) == false)
            {
               throw new IllegalStateException("Integration target mismatch, using: " + jbossVersion);
               //integrationTarget = jbossVersion;
            }
         }
         catch (Throwable th)
         {
            // ignore, we are not running on jboss-4.2 or greater
         }
      }
      return integrationTarget;
   }

   /** Try to discover the URL for the deployment archive */
   public URL getArchiveURL(String archive) throws MalformedURLException
   {
      URL url = null;
      try
      {
         url = new URL(archive);
      }
      catch (MalformedURLException ignore)
      {
         // ignore
      }

      if (url == null)
      {
         File file = new File(archive);
         if (file.exists())
            url = file.toURL();
      }

      if (url == null)
      {
         File file = new File("libs/" + archive);
         if (file.exists())
            url = file.toURL();
      }

      if (url == null)
         throw new IllegalArgumentException("Cannot obtain URL for: " + archive);

      return url;
   }
}