//-----------------------------------------------------------------------------
// File Name        : Terminal.cs
// Project          : TSC eCommerce Interface
// Creation Date    : 04/11/2020
// Creation Author  : Stefano Bertarello - Simone Sambruni
//-----------------------------------------------------------------------------
// Copyright(C) Greencore srl 2020

using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using System.Data.Entity;

namespace EComArsInterface.Models
{
    // Ean
    public class Ean
    {

        public decimal Price { get; set; }

        public string Code { get; set; }

        public string Department { get; set; }

        public string Description { get; set; }

    }

}