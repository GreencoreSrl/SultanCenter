namespace EComArsInterface.Migrations
{
    using System;
    using System.Data.Entity.Migrations;
    
    public partial class ChangeUnitPriceTypeFromStringToDecimalNullable : DbMigration
    {
        public override void Up()
        {
            AlterColumn("dbo.DATA_ITEM", "UnitPrice", c => c.Decimal(precision: 18, scale: 2));
        }
        
        public override void Down()
        {
            AlterColumn("dbo.DATA_ITEM", "UnitPrice", c => c.Decimal(nullable: false, precision: 18, scale: 2));
        }
    }
}
