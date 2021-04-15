﻿//-------------------------------------     ----------------------------------------
// File Name        : BasketController.cs
// Project          : TSC eCommerce 
// Creation Date    : 09/09/2020
// Creation Author  : Stefano Bertarello - Simone Sambruni
//-----------------------------------------------------------------------------
// Copyright(C) Greencore srl 2020


using EComArsInterface.Models;
using Newtonsoft.Json;
using System.Collections.Generic;
using System.Data.Entity;
using System.Data.Entity.Infrastructure;
using System.Linq;
using System.Net;
using System.Web.Http;
using System.Web.Http.Description;

namespace EComArsInterface
{
    public class BasketController : ApiController
    {
        private readonly ARSDBContext db = new ARSDBContext();
        private static readonly NLog.Logger _log = NLog.LogManager.GetCurrentClassLogger();
        
        // SYNTAX GET: api/Basket
        [ResponseType(typeof(Basket))]
        public IHttpActionResult GetBasket(string BasketId)
        {
            _log.Trace("GetBasketID - Start");

            Basket basket = db.Baskets.Include(i => i.Items).Include(i => i.SoldItems).Include(i => i.NotSoldItems).Where(b => b.BasketID.Trim().Equals(BasketId.Trim())).FirstOrDefault();
            if (basket == null)
            {
                _log.Info("Basket not found!");
                return NotFound();
            }

            try
            {
                if (basket != null)
                {
                    basket.Status = "Processing";
                    db.Entry(basket).State = EntityState.Modified;
                    db.SaveChanges();
                }
            }
            catch (DbUpdateConcurrencyException ex)
            {
                _log.Error("Exception: " + ex.Message);
                throw;
            }

            _log.Trace("GetBasketID - End");

            return Ok(basket);
        }


        // SYNTAX GET: api/Basket
        [ResponseType(typeof(Basket))]
        public IHttpActionResult GetTerminal(string TerminalId)
        {
            _log.Trace("GetBasketID - Start");
            Basket basket = null;

            //var tmp = db.Baskets.ToList();
            basket = db.Baskets.Include(i => i.Items).Include(i => i.SoldItems).Include(i => i.NotSoldItems).Where(b => b.Status.Trim().Equals("Received")).FirstOrDefault();

            if (basket == null)
            {
                _log.Info("Basket not found!");
                return NotFound();
            }

            _log.Trace("GetBasketID - End");

            return Ok(basket);
        }


        // SYNTAX PUT: api/Basket        
        [ResponseType(typeof(void))]
        public IHttpActionResult PutBasket(Basket basket)
        {
            _log.Trace("PutBasket - Start");

            if (!ModelState.IsValid)
            {
                _log.Info("Bad request!");
                return BadRequest(ModelState);
            }

            // Check if BasketId is already present into database
            Basket obj = db.Baskets.Include(i => i.Items).Include(i => i.SoldItems).Include(i => i.NotSoldItems).Where(b => (b.BasketID.Trim().Equals(basket.BasketID.Trim()) && b.Type.Trim().Equals(basket.Type.Trim()))).FirstOrDefault();
            if (obj != null)
            {
                _log.Info("Basket already exists!");
                return BadRequest();
            }

            // Set necessary field to send response
            try
            {
                // Set state to Received only for type Sale
                basket.Status = "Received";
                basket.TerminalID = "";
                basket.Receipt = "";
                basket.TotalAmount = 0.0M;
                basket.EarnedLoyaltyPoints = 0;
                basket.TransactionId = "";
                basket.BarcodeId = "";
                basket.SoldItems = null;
                basket.NotSoldItems = null;
                basket.TenderType = "Online";
                basket.OriginBasketId = "";
                basket.ErrorCode = 0;

                db.Entry(basket).State = EntityState.Added;
                db.SaveChanges();
            }
            catch (DbUpdateConcurrencyException ex)
            {
                _log.Error("Exception: " + ex.Message);
                throw;
            }

            _log.Trace("PutBasket - End");
            return Ok(basket);
        }


        // SYNTAX POST: api/Basket
        [ResponseType(typeof(Basket))]
        public IHttpActionResult PostBasket(Basket basket)
        {
            _log.Trace("PostBasket - Start");

            if (!ModelState.IsValid)
            {
                _log.Info("Bad request!");
                return BadRequest(ModelState);
            }

            if (!BasketExists(basket.BasketID, basket.Type))
            {
                _log.Info("Basket not found!");
                return NotFound();
            }

            if (basket.Status.Trim().Equals("Processing"))
            {
                _log.Info("Basket status is Processing!");
                return BadRequest();
            }
            else
            {
                Basket objToUpdate = db.Baskets.Include(i => i.Items).Include(i => i.SoldItems).Include(i => i.NotSoldItems).Where(b => b.BasketID.Trim().Equals(basket.BasketID.Trim()) && b.Type.Trim().Equals(basket.Type.Trim())).First();
                if (objToUpdate != null)
                {
                    objToUpdate.Status = basket.Status;
                    objToUpdate.TerminalID = basket.TerminalID;
                    objToUpdate.CustomerID = basket.CustomerID;
                    objToUpdate.BarcodeId = basket.BarcodeId;
                    objToUpdate.EarnedLoyaltyPoints = basket.EarnedLoyaltyPoints;
                    objToUpdate.OriginBasketId = basket.OriginBasketId;
                    objToUpdate.Type = basket.Type;
                    objToUpdate.TenderType = basket.TenderType;
                    objToUpdate.TotalAmount = basket.TotalAmount;
                    objToUpdate.TransactionId = basket.TransactionId;                  
                    objToUpdate.ErrorCode = basket.ErrorCode;
                    objToUpdate.Receipt = basket.Receipt;

                    foreach (Item item in basket.Items)
                    {
                        Item current = objToUpdate.Items.Where(i => i.Code == item.Code).FirstOrDefault();
                        if (current == null)
                        {
                            objToUpdate.Items.Add(item);
                        }
                        else
                        {
                            current.Code = item.Code;
                            current.Barcode = item.Barcode;
                            current.Qty = item.Qty;
                            current.Price = item.Price;
                            current.UnitPrice = item.UnitPrice;
                        }
                    }

                    foreach (SoldItem item in basket.SoldItems)
                    {
                        SoldItem current = objToUpdate.SoldItems.Where(i => i.Code == item.Code).FirstOrDefault();
                        if (current == null)
                        {
                            objToUpdate.SoldItems.Add(item);
                        }
                        else
                        {
                            current.Code = item.Code;
                            current.Barcode = item.Barcode;
                            current.Qty = item.Qty;
                            current.Price = item.Price;
                            current.UnitPrice = item.UnitPrice;
                        }

                    }

                    foreach (NotSoldItem item in basket.NotSoldItems)
                    {
                        NotSoldItem current = objToUpdate.NotSoldItems.Where(i => i.Code == item.Code).FirstOrDefault();
                        if (current == null)
                        {
                            objToUpdate.NotSoldItems.Add(item);
                        }
                        else
                        {
                            current.Code = item.Code;
                            current.Barcode = item.Barcode;
                            current.Qty = item.Qty;
                            current.Price = item.Price;
                            current.UnitPrice = item.UnitPrice;
                        }

                    }
                }
            }
            try
            {
                db.SaveChanges();
            }
            catch (DbUpdateException ex)
            {
                _log.Error("Exception: " + ex.Message);
                throw;
            }

            _log.Trace("PostBasket - End");

            return Ok(basket);
        }


        // SYNTAX DELETE: api/Basket/id/type
        public IHttpActionResult DeleteBasket(string basketId, string type)
        {
            _log.Trace("DeleteBasket - Start");

            if (basketId != "ALL")
            {
                Basket basket = db.Baskets.Include(i => i.Items).Include(i => i.SoldItems).Include(i => i.NotSoldItems).Where(b=>(b.BasketID.Equals(basketId) && b.Type.Equals(type))).FirstOrDefault();
                if (basket == null)
                {
                    _log.Info("Basket not found!");
                    return BadRequest();
                }

                if (basket.Status != "Processing")
                {
                    // To preserve the relationship object save a JSON string...
                    string jsonBasket = JsonConvert.SerializeObject(basket);
                    db.Baskets.Remove(basket);
                    db.SaveChanges();

                    _log.Trace("Deleting basket successed");
                    _log.Trace("DeleteBasket - End");
                    return Content(HttpStatusCode.OK, JsonConvert.DeserializeObject<Basket>(jsonBasket));
                }
                else
                {
                    _log.Trace("Basket is in Processing status");
                    _log.Trace("DeleteBasket - End");
                    return Content(HttpStatusCode.BadRequest, "");
                }
            }
            else {

                string JsonBasketsList = string.Empty;
                List<Basket> basketList = db.Baskets.Include(i => i.Items).Include(i => i.SoldItems).Include(i => i.NotSoldItems).Where(b => b.Status == "Received").ToList<Basket>();
                if (basketList != null && basketList.Count > 0)
                {
                    JsonBasketsList = JsonConvert.SerializeObject(basketList);
                    foreach (Basket item in basketList)
                    {
                        db.Baskets.Remove(item);
                    }
                    db.SaveChanges();
                }

                _log.Trace("All basket are deleted success");
                _log.Trace("DeleteBasket - End");
                return Content(HttpStatusCode.OK, JsonConvert.DeserializeObject<List<Basket>>(JsonBasketsList));
            }
        }


        protected override void Dispose(bool disposing)
        {
            if (disposing)
            {
                db.Dispose();
            }
            base.Dispose(disposing);
        }


        private bool BasketExists(string id, string type)
        {
            return db.Baskets.Count(e => e.BasketID == id && e.Type ==  type) > 0;
        }
    }
}