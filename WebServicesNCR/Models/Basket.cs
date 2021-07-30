//-----------------------------------------------------------------------------
// File Name        : Basket.cs
// Project          : TSC eCommerce Interface
// Creation Date    : 09/09/2020
// Creation Author  : Stefano Bertarello - Simone Sambruni
//-----------------------------------------------------------------------------
// Copyright(C) Greencore srl 2020

using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace EComArsInterface.Models
{

    // Basket DBContext model class
    public class Basket
    {
        //public Basket()
        //{
        //    this.CreatedDate = DateTime.UtcNow;

        //}
        [Key]
        [JsonIgnore]
        public int ID { get; set; }
        public string BasketID { get; set; }
        public string Status { get; set; }
        public string TerminalID { get; set; }
        public string CustomerID { get; set; }
        public string Type { get; set; }
        public string Receipt { get; set; }
        public decimal TotalAmount { get; set; }
        public int EarnedLoyaltyPoints { get; set; }
        public string TransactionId { get; set; }
        public string BarcodeId { get; set; }
        public List<Item> Items { get; set; }
        public List<SoldItem> SoldItems { get; set; }
        public List<NotSoldItem> NotSoldItems { get; set; }
        public string OriginBasketId { get; set; }
        public string TenderType { get; set; }
        ///*
        // * Edit By : Soukaina Mouatassim
        // * Description: Add TenderId to handle others payment types
        // * **/
        public string TenderId { get; set; }
        public int ErrorCode { get; set; }

        [JsonIgnore]
        [DatabaseGenerated(DatabaseGeneratedOption.Computed)]
        [DefaultValue("getutcdate()")]

        public DateTime CreatedDate { get; set; }
    }


    // Item DBContext model class
    public class Item
    {
        [Key]
        [JsonIgnore]
        public int ID { get; set; }
        public string Code { get; set; }
        public string Qty { get; set; }
        //public string UnitPrice { get; set; }
        [JsonIgnore]
        private decimal? _unitPrice { get; set; }
        public decimal? UnitPrice {
            get { return _unitPrice; }
            set
            {
                _unitPrice = value;
                if (value == null)
                    _unitPrice = 0;
            }
       
        }
        public string Barcode { get; set; }
        public decimal Price { get; set; }
    }


    // SoldItem DBContext model class
    public class SoldItem
    {
        [Key]
        [JsonIgnore]
        public int ID { get; set; }
        public string Code { get; set; }
        public string Qty { get; set; }
       
        public decimal UnitPrice { get; set; }
        public string Barcode { get; set; }
        public decimal Price { get; set; }
    }


    // NotSoldItem DBContext model class
    public class NotSoldItem
    {
        [Key]
        [JsonIgnore]
        public int ID { get; set; }
        public string Code { get; set; }
        public string Qty { get; set; }
        //public string UnitPrice { get; set; }
        //[JsonIgnore]
        //private string _unitPrice;
        //public string UnitPrice
        //{
        //    get
        //    {
        //        return _unitPrice;
        //    }
        //    set
        //    {
        //        if (value == null)
        //        {
        //            _unitPrice = "";
        //        }
        //        else
        //        {
        //            _unitPrice = value;
        //        }
        //    }
        //}
        public decimal UnitPrice { get; set; }
        public string Barcode { get; set; }
        public decimal Price { get; set; }
    }

}
