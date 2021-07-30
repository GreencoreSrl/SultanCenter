//-----------------------------------------------------------------------------
// File Name        : ARSDBContext.cs
// Project          : TSC eCommerce 
// Creation Date    : 09/09/2020
// Creation Author  : Stefano Bertarello - Simone Sambruni
//-----------------------------------------------------------------------------
// Copyright(C) Greencore srl 2020


using System.Data.Entity;
using System.Web;

namespace EComArsInterface.Models
{
    public class ARSDBContext : DbContext
    {
        public ARSDBContext()
           : base("DefaultConnection")
        {
        }

        public DbSet<Basket> Baskets { get; set; }
        public DbSet<Item> Items { get; set; }
        public DbSet<SoldItem> SoldItems { get; set; }
        public DbSet<NotSoldItem> NotSoldItems { get; set; }
        public DbSet<Terminal> Terminals { get; set; }

        protected override void OnModelCreating(DbModelBuilder modelBuilder)
        {

            modelBuilder.Entity<Basket>().HasMany(b => b.Items).WithRequired().WillCascadeOnDelete(true);
            modelBuilder.Entity<Basket>().HasMany(b => b.SoldItems).WithRequired().WillCascadeOnDelete(true);
            modelBuilder.Entity<Basket>().HasMany(b => b.NotSoldItems).WithRequired().WillCascadeOnDelete(true);

            modelBuilder.Entity<Basket>().Property(x => x.TotalAmount).HasPrecision(18, 3);
            modelBuilder.Entity<Item>().Property(x => x.UnitPrice).HasPrecision(18, 3);
            modelBuilder.Entity<Item>().Property(x => x.Price).HasPrecision(18, 3);
            modelBuilder.Entity<Basket>().Property(x => x.CreatedDate).HasDatabaseGeneratedOption(System.ComponentModel.DataAnnotations.Schema.DatabaseGeneratedOption.Computed);
            modelBuilder.Entity<SoldItem>().Property(x => x.UnitPrice).HasPrecision(18, 3);
            modelBuilder.Entity<SoldItem>().Property(x => x.Price).HasPrecision(18, 3);
            modelBuilder.Entity<NotSoldItem>().Property(x => x.UnitPrice).HasPrecision(18, 3);
            modelBuilder.Entity<NotSoldItem>().Property(x => x.Price).HasPrecision(18, 3);
        


            modelBuilder.Entity<Basket>().ToTable("DATA_BASKET");
            modelBuilder.Entity<Item>().ToTable("DATA_ITEM");
            modelBuilder.Entity<SoldItem>().ToTable("DATA_SOLDITEM");
            modelBuilder.Entity<NotSoldItem>().ToTable("DATA_NOTSOLDITEM");
            modelBuilder.Entity<Terminal>().ToTable("DATA_TERMINAL");
        }       
    }
}