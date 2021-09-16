namespace EComArsInterface.Migrations
{
    using System;
    using System.Data.Entity.Migrations;
    
    public partial class Ecommerce_update2 : DbMigration
    {
        public override void Up()
        {
            CreateTable(
                "dbo.DATA_EXTRAITEM",
                c => new
                    {
                        ID = c.Int(nullable: false, identity: true),
                        Code = c.String(),
                        Qty = c.String(),
                        UnitPrice = c.Decimal(nullable: false, precision: 18, scale: 3),
                        Barcode = c.String(),
                        Price = c.Decimal(nullable: false, precision: 18, scale: 3),
                        Basket_ID = c.Int(nullable: false),
                    })
                .PrimaryKey(t => t.ID)
                .ForeignKey("dbo.DATA_BASKET", t => t.Basket_ID, cascadeDelete: true)
                .Index(t => t.Basket_ID);
            
        }
        
        public override void Down()
        {
            DropForeignKey("dbo.DATA_EXTRAITEM", "Basket_ID", "dbo.DATA_BASKET");
            DropIndex("dbo.DATA_EXTRAITEM", new[] { "Basket_ID" });
            DropTable("dbo.DATA_EXTRAITEM");
        }
    }
}
