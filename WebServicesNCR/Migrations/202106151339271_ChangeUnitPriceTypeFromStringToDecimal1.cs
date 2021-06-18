namespace EComArsInterface.Migrations
{
    using System;
    using System.Data.Entity.Migrations;
    
    public partial class ChangeUnitPriceTypeFromStringToDecimal1 : DbMigration
    {
        public override void Up()
        {
            AlterColumn("dbo.DATA_ITEM", "UnitPrice", c => c.Decimal(nullable: false, precision: 18, scale: 2));
            AlterColumn("dbo.DATA_NOTSOLDITEM", "UnitPrice", c => c.Decimal(nullable: false, precision: 18, scale: 2));
            AlterColumn("dbo.DATA_SOLDITEM", "UnitPrice", c => c.Decimal(nullable: false, precision: 18, scale: 2));
        }
        
        public override void Down()
        {
            AlterColumn("dbo.DATA_SOLDITEM", "UnitPrice", c => c.String());
            AlterColumn("dbo.DATA_NOTSOLDITEM", "UnitPrice", c => c.String());
            AlterColumn("dbo.DATA_ITEM", "UnitPrice", c => c.String());
        }
    }
}
