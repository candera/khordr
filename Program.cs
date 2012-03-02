using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace kchordr
{
    class Program
    {
        static void Main(string[] args)
        {
            IntPtr context;
            int device;
            Interception.Stroke stroke = new Interception.Stroke();

            context = Interception.CreateContext();

            Interception.SetFilter(context, Interception.IsKeyboard, Interception.Filter.All);

            while (Interception.Receive(context, device = Interception.Wait(context), ref stroke, 1) > 0)
            {
                Console.WriteLine("SCAN CODE: {0}/{1}", stroke.key.code, stroke.key.state);

                if (stroke.key.code == ScanCode.X) 
                {
                    stroke.key.code = ScanCode.Y;
                }
                Interception.Send(context, device, ref stroke, 1);

                // Hitting escape terminates the program
                if (stroke.key.code == ScanCode.Escape)
                {
                    break;
                }
            }

            Interception.DestroyContext(context);
        }
    }
}
