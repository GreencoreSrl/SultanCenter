//-----------------------------------------------------------------------------
// File Name        : WebApiConfig.cs
// Project          : TSC eCommerce 
// Creation Date    : 09/09/2020
// Creation Author  : Stefano Bertarello - Simone Sambruni
//-----------------------------------------------------------------------------
// Copyright(C) Greencore srl 2020


using System.Web.Http;

namespace EComArsInterface
{
    public static class WebApiConfig
    {
        public static void Register(HttpConfiguration config)
        {
            // Web API routes
            config.MapHttpAttributeRoutes();

            // Web API configuration and services
            // Heartbeat Route
            config.Routes.MapHttpRoute(
                name: "Hearbeat",
                routeTemplate: "api/Hearbeat",
                defaults: new { controller = "Hearbeat", id = RouteParameter.Optional }
            );

            // Status Route
            config.Routes.MapHttpRoute(
                name: "Status",
                routeTemplate: "api/Status",
                defaults: new { controller = "Status", id = RouteParameter.Optional }
            );

            // Default Routes
            config.Routes.MapHttpRoute(
                name: "DefaultApiBasketIdType",
                routeTemplate: "api/{controller}/{BasketId}/{Type}",
                defaults: new { BasketId = RouteParameter.Optional, Type = RouteParameter.Optional }
            );

            config.Routes.MapHttpRoute(
                name: "DefaultApiBasketId",
                routeTemplate: "api/{controller}/{BasketId}",
                defaults: new { BasketId = RouteParameter.Optional }
            );

            config.Routes.MapHttpRoute(
                name: "DefaultApiTerminalId",
                routeTemplate: "api/{controller}/{TerminalId}",
                defaults: new { TerminalId = RouteParameter.Optional }
            );

        }
    }
}
