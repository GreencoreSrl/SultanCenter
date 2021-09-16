//-------------------------------------     ----------------------------------------
// File Name        : SearchPLUController.cs
// Project          : TSC eCommerce 
// Creation Date    : 20/07/2021
// Creation Author  : Stefano Bertarello - Simone Sambruni
//-----------------------------------------------------------------------------
// Copyright(C) Greencore srl 2021


using EComArsInterface.Models;
using System;
using System.Configuration;
using System.IO;
using System.IO.MemoryMappedFiles;
using System.Text;
using System.Web.Http;
using System.Web.Http.Description;
using System.Web.UI.WebControls;

namespace EComArsInterface
{
	public class SearchPLUController : ApiController
    {
		private string pb = string.Empty;
		private static int fixSize = 16;
        private static string DAT_FILE = "M_HSHPLU.DAT";
        private static readonly NLog.Logger _log = NLog.LogManager.GetCurrentClassLogger();

		// SYNTAX GET: api/SearchPLU
		[ResponseType(typeof(Ean))]
        public IHttpActionResult GetSearchPLU(string Ean)
        {
            _log.Trace("SearchPLU - Start");
			Ean obj = null;

			// Checking of EAN code...
			if (string.IsNullOrEmpty(Ean))
            {
              
                return BadRequest("Ean code is not valid ");
            }

			if (find_code(Ean.PadLeft(16, ' ')) > 0 && !string.IsNullOrEmpty(pb))
			{
				_log.Trace("Ean product found!");
				obj = new Ean();
				obj.Code = Ean;
				obj.Department = pb.Substring(16, 4);
				obj.Description = pb.Substring(36, 20);
				obj.Price = int.Parse(pb.Substring(70, 8));
			}
			else return NotFound();

            _log.Trace("SearchPLU - End");
            return Ok(obj);
            
        }


		private String leftFill(String s, int len, char prefix)
		{
			int ind = s.Length - len;
			if (ind >= 0)
				return s.Substring(ind, ind + len);

			StringBuilder sb = new StringBuilder(len);
			while (ind++ < 0)
				sb.Append(prefix);
			return sb.Append(s).ToString();
		}


		private int read(long rec, FileStream file)
		{
			byte[] record = new byte[80];

			if (file == null)
				return 0;

			try
			{
				using (MemoryMappedFile memoryMapped = MemoryMappedFile.CreateFromFile(file, "MapDATFile", file.Length,
					MemoryMappedFileAccess.Read, new MemoryMappedFileSecurity(), HandleInheritability.Inheritable, true))
				{
					var viewStream = memoryMapped.CreateViewStream(0, file.Length, MemoryMappedFileAccess.Read);

					while (true)
					{
						int len = record.Length;
						viewStream.Seek((rec - 1L) * len, SeekOrigin.Begin);
						viewStream.Read(record, 0, record.Length);
						if (record[--len] == 0x0a)
							if (record[--len] == 0x0d)
							{
								pb = System.Text.Encoding.UTF8.GetString(record, 0, len);
								if ( viewStream != null)
								{
									viewStream.Close();
									viewStream.Dispose();
								}

								if (memoryMapped != null) memoryMapped.Dispose();
								return len;
							}
						break;
					}
				}
			}
			catch (IOException ex)
			{
				_log.Error("Read - Exception occurred: " + ex.StackTrace);
				return 0;
			}

			return 0;
		}


		private int find_code(string key)
        {
			FileStream fs = new FileStream(ConfigurationManager.AppSettings["PathFileDAT"] + "\\" + DAT_FILE, FileMode.Open, FileAccess.Read);			

			long recno = 0;
			long top = 0, del = 0, end = (fs.Length / 80);
			if (end < 8)
			{
				if (fs != null) fs.Close();
				return -1;
			}

			
			int ind = fixSize + 5 & ~7;
			int val = (int)key[1] & 0x0f;
			string s = leftFill(key.Substring(2), ind, ' ');
		
			while (val-- > 0)
				s = s.Substring(1) + s[0];
			
			for (char [] tmp = new char[8]; ind-- > 0;)
			{
				val = s[ind] & 0x0f;
				if (val > 9)
					val -= 6;
				tmp[ind & 7] = (char)(val + '0');
				if ((ind & 7) > 0)
					continue;
				top += int.Parse(new String(tmp));
			}

			top %= end >> 3;
			recno = top <<= 3;

			while (read(++recno, fs) > 0)
			{
				char c = pb[(fixSize - 1)];
				if (c < '0')
				{
					if (del == 0)
						del = recno;
					if (c == ' ')
						break;
				}
				else if (pb.StartsWith(key))
					{
						if (fs != null) fs.Close();
						return pb.Length;
					}

					if (recno == end)
						recno = 0;
				if (recno == top)
					break;
			}

			if ((recno = del) > 0)
			{
				if (fs != null) fs.Close();
				return 0;
			}

			if (fs != null) fs.Close();
			return -1;
		
		}
    }
}