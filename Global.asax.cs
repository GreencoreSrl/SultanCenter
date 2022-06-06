using NLog;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Web;
using System.Web.Http;
using System.Web.Routing;

namespace EComArsInterface
{
    public class WebApiApplication : System.Web.HttpApplication
    {
        protected void Application_Start()
        {
           /* var lConfig = new NLog.Config.LoggingConfiguration();
            // Targets where to log to: File and Console
            var logfile = new NLog.Targets.FileTarget("logfile") { Layout = "${longdate}|${level}|${logger}|${message} ${exception:format=tostring}" , FileName = "EComInterface.log" };
            //var logfile = new NLog.Targets.FileTarget("logfile") {  FileName = "EComInterface.log" };
            var logconsole = new NLog.Targets.ConsoleTarget("logconsole");
            
            // Rules for mapping loggers to targets            
            lConfig.AddRule(LogLevel.Info, LogLevel.Fatal, logconsole);
            lConfig.AddRule(LogLevel.Trace, LogLevel.Fatal, logfile);

            // Apply config           
            NLog.LogManager.Configuration = lConfig;*/

            GlobalConfiguration.Configure(WebApiConfig.Register);
        }
    }
}
