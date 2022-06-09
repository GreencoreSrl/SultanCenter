//-----------------------------------------------------------------------------
// File Name        : Heartbeat.cs
// Project          : TSC eCommerce Interface
// Creation Date    : 04/11/2020
// Creation Author  : Stefano Bertarello - Simone Sambruni
//-----------------------------------------------------------------------------
// Copyright(C) Greencore srl 2020

using System;
using System.Data;
using System.Data.Entity;
using System.Data.Entity.Infrastructure;
using System.Linq;
using System.Threading.Tasks;
using System.Web.Http;
using System.Web.Http.Description;
using EComArsInterface.Models;

namespace EComArsInterface.Controllers
{
    public class HeartbeatController : ApiController
    {
        private readonly ARSDBContext db = new ARSDBContext();
        private static readonly NLog.Logger _log = NLog.LogManager.GetCurrentClassLogger();

        // POST: api/Heartbeat
        [ResponseType(typeof(Terminal))]
        public async Task<IHttpActionResult> PostHeartbeat(Terminal terminal)
        {
            _log.Trace($"PostHeartbeat: Method - PostHeartbeat(terminal = { terminal.TerminalId}) - Start");

            if (!ModelState.IsValid)
            {
                _log.Error("PostHeartbeat: Method - PostHeartbeat(terminal = { terminal.TerminalId}). Result: Bad request!");
                return BadRequest(ModelState);
            }

            Terminal objToUpdate = db.Terminals.Where(h => h.TerminalId == terminal.TerminalId).FirstOrDefault();
            if (objToUpdate != null)
            {
                objToUpdate.TerminalId = terminal.TerminalId;
                objToUpdate.ErrorCode = terminal.ErrorCode;
                objToUpdate.CheckDate = DateTime.Now;
            }
            else {
                terminal.CheckDate = DateTime.Now;
                db.Terminals.Add(terminal);
            }

            try
            {
                await db.SaveChangesAsync();
            }
            catch (DbUpdateException ex)
            {
                _log.Error("Exception: " + ex.Message);
                throw;
            }

            _log.Trace($"PostHeartbeat: Method - PostHeartbeat(terminal = { terminal.TerminalId}) - End");
            return Ok(terminal);
        }

        protected override void Dispose(bool disposing)
        {
            if (disposing)
            {
                db.Dispose();
            }
            base.Dispose(disposing);
        }

        private bool HeartbeatExists(string id)
        {
            return db.Terminals.Count(e => e.TerminalId == id) > 0;
        }
    }
}