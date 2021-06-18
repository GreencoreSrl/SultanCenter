namespace EComArsInterface.Migrations
{
    using System;
    using System.Data.Entity.Migrations;
    
    public partial class TenderIdAdded : DbMigration
    {
        public override void Up()
        {
            AddColumn("dbo.DATA_BASKET", "TenderId", c => c.String());
        }
        
        public override void Down()
        {
            DropColumn("dbo.DATA_BASKET", "TenderId");
        }
    }
}
