namespace EComArsInterface.Migrations
{
    using System;
    using System.Data.Entity.Migrations;
    
    public partial class Ecommerce_update : DbMigration
    {
        public override void Up()
        {
            AddColumn("dbo.DATA_BASKET", "Source", c => c.String());
        }
        
        public override void Down()
        {
            DropColumn("dbo.DATA_BASKET", "Source");
        }
    }
}
