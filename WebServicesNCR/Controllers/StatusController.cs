//-----------------------------------------------------------------------------
// File Name        : StatusController.cs
// Project          : TSC eCommerce Interface
// Creation Date    : 04/11/2020
// Creation Author  : Stefano Bertarello - Simone Sambruni
//-----------------------------------------------------------------------------
// Copyright(C) Greencore srl 2020

using System;
using System.Collections.Generic;
using System.Configuration;
using System.Data.Entity;
using System.Linq;
using System.Web.Http;
using System.Web.UI.WebControls;
using EComArsInterface.Models;

namespace EComArsInterface.Controllers
{
    public class TermStatus
    {
        public List<Terminal> Terminals { get; set; }
        public List<Basket> ActiveBaskets { get; set; }
    }

    public class StatusController : ApiController
    {
        private readonly ARSDBContext db = new ARSDBContext();
        private static readonly NLog.Logger _log = NLog.LogManager.GetCurrentClassLogger();

        // GET: api/Status
        public IQueryable<TermStatus> GetTerminals()
        {
            _log.Trace("GetTerminals");            

            List<TermStatus> objList = new List<TermStatus>();
            objList.Add(new TermStatus()
            {
                Terminals = BuildTerminalList(),
                ActiveBaskets = new List<Basket>(db.Baskets.Include(i => i.Items).Include(i => i.SoldItems).Include(i => i.NotSoldItems).Where(b => b.Status == "Processing").AsEnumerable<Basket>())
            });

            return objList.AsQueryable<TermStatus>();
        }

        private List<Terminal> BuildTerminalList()
        {
            List<Terminal> term = new List<Terminal>();
            foreach (Terminal t in db.Terminals)
            {
                if (DateTime.Now.Subtract(t.CheckDate).TotalMinutes > Convert.ToDouble(ConfigurationManager.AppSettings["HBTimeIsOver"]))
                    t.Status = "Unreachable";
                else
                    t.Status = "Ready";

                term.Add(t);
            }

            return term;
        }

        protected override void Dispose(bool disposing)
        {
            if (disposing)
            {
                db.Dispose();
            }
            base.Dispose(disposing);
        }
    }
}