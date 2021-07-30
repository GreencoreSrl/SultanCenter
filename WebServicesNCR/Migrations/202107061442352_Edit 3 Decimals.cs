namespace EComArsInterface.Migrations
{
    using System;
    using System.Data.Entity.Migrations;
    
    public partial class Edit3Decimals : DbMigration
    {
        public override void Up()
        {
            CreateTable(
                "dbo.DATA_BASKET",
                c => new
                    {
                        ID = c.Int(nullable: false, identity: true),
                        BasketID = c.String(),
                        Status = c.String(),
                        TerminalID = c.String(),
                        CustomerID = c.String(),
                        Type = c.String(),
                        Receipt = c.String(),
                        TotalAmount = c.Decimal(nullable: false, precision: 18, scale: 3),
                        EarnedLoyaltyPoints = c.Int(nullable: false),
                        TransactionId = c.String(),
                        BarcodeId = c.String(),
                        OriginBasketId = c.String(),
                        TenderType = c.String(),
                        TenderId = c.String(),
                        ErrorCode = c.Int(nullable: false),
                        CreatedDate = c.DateTime(nullable: false),
                    })
                .PrimaryKey(t => t.ID);
            
            CreateTable(
                "dbo.DATA_ITEM",
                c => new
                    {
                        ID = c.Int(nullable: false, identity: true),
                        Code = c.String(),
                        Qty = c.String(),
                        UnitPrice = c.Decimal(precision: 18, scale: 3),
                        Barcode = c.String(),
                        Price = c.Decimal(nullable: false, precision: 18, scale: 3),
                        Basket_ID = c.Int(nullable: false),
                    })
                .PrimaryKey(t => t.ID)
                .ForeignKey("dbo.DATA_BASKET", t => t.Basket_ID, cascadeDelete: true)
                .Index(t => t.Basket_ID);
            
            CreateTable(
                "dbo.DATA_NOTSOLDITEM",
                c => new
                    {
                        ID = c.Int(nullable: false, identity: true),
                        Code = c.String(),
                        Qty = c.String(),
                        UnitPrice = c.Decimal(nullable: false, precision: 18, scale: 2),
                        Barcode = c.String(),
                        Price = c.Decimal(nullable: false, precision: 18, scale: 2),
                        Basket_ID = c.Int(nullable: false),
                    })
                .PrimaryKey(t => t.ID)
                .ForeignKey("dbo.DATA_BASKET", t => t.Basket_ID, cascadeDelete: true)
                .Index(t => t.Basket_ID);
            
            CreateTable(
                "dbo.DATA_SOLDITEM",
                c => new
                    {
                        ID = c.Int(nullable: false, identity: true),
                        Code = c.String(),
                        Qty = c.String(),
                        UnitPrice = c.Decimal(nullable: false, precision: 18, scale: 2),
                        Barcode = c.String(),
                        Price = c.Decimal(nullable: false, precision: 18, scale: 2),
                        Basket_ID = c.Int(nullable: false),
                    })
                .PrimaryKey(t => t.ID)
                .ForeignKey("dbo.DATA_BASKET", t => t.Basket_ID, cascadeDelete: true)
                .Index(t => t.Basket_ID);
            
            CreateTable(
                "dbo.DATA_TERMINAL",
                c => new
                    {
                        TerminalId = c.String(nullable: false, maxLength: 128),
                        ErrorCode = c.Int(nullable: false),
                        Status = c.String(),
                        CheckDate = c.DateTime(nullable: false),
                    })
                .PrimaryKey(t => t.TerminalId);
            
        }
        
        public override void Down()
        {
            DropForeignKey("dbo.DATA_SOLDITEM", "Basket_ID", "dbo.DATA_BASKET");
            DropForeignKey("dbo.DATA_NOTSOLDITEM", "Basket_ID", "dbo.DATA_BASKET");
            DropForeignKey("dbo.DATA_ITEM", "Basket_ID", "dbo.DATA_BASKET");
            DropIndex("dbo.DATA_SOLDITEM", new[] { "Basket_ID" });
            DropIndex("dbo.DATA_NOTSOLDITEM", new[] { "Basket_ID" });
            DropIndex("dbo.DATA_ITEM", new[] { "Basket_ID" });
            DropTable("dbo.DATA_TERMINAL");
            DropTable("dbo.DATA_SOLDITEM");
            DropTable("dbo.DATA_NOTSOLDITEM");
            DropTable("dbo.DATA_ITEM");
            DropTable("dbo.DATA_BASKET");
        }
    }
}
