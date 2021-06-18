namespace EComArsInterface.Migrations
{
    using System;
    using System.Data.Entity.Migrations;
    
    public partial class ChangeUnitPriceTypeRollBack : DbMigration
    {
        public override void Up()
        {
            AlterColumn("dbo.DATA_ITEM", "UnitPrice", c => c.String());
        }
        
        public override void Down()
        {
            AlterColumn("dbo.DATA_ITEM", "UnitPrice", c => c.Decimal(precision: 18, scale: 2));
        }
    }
}
