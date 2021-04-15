using System;
using System.Collections.Generic;
using System.Configuration;
using System.Data.SqlClient;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Web.Http;

namespace EComArsInterface.Controllers
{
    public class ValuesController : ApiController
    {

        // GET api/<controller>/5
        public HttpResponseMessage Get(int id)
        {
            using (SqlConnection conn = new SqlConnection())
            {
                conn.ConnectionString = "Data Source = WIN10X64SS; Initial Catalog = tempdb; Integrated Security = True";
                // using the code here...

                //ConfigurationManager.ConnectionStrings
                conn.Open();

                if (conn.State == System.Data.ConnectionState.Open)
                    return Request.CreateResponse(HttpStatusCode.OK, "Connection SQL OK");
            }



            return Request.CreateResponse(HttpStatusCode.BadRequest, "Connection SQL FAILED");
        }

        // POST api/<controller>
        public HttpResponseMessage Post(HttpRequestMessage request)
        {
            var value = request.Content.ReadAsStringAsync().Result;

            return Request.CreateResponse(HttpStatusCode.OK);
        }

        // PUT api/<controller>/5
        public HttpResponseMessage Put(int id, HttpRequestMessage request)
        {
            if (id == 0)
            {
                return Request.CreateErrorResponse(HttpStatusCode.BadRequest, "Basket id is not valid");
            }

            return Request.CreateResponse(HttpStatusCode.OK);
        }

        // DELETE api/<controller>/5
        public HttpResponseMessage Delete(int id)
        {

            if (id == 0)
            {
                return Request.CreateErrorResponse(HttpStatusCode.BadRequest, "Basket id is not valid");
            }

            return Request.CreateResponse(HttpStatusCode.OK);
        }

    }
}