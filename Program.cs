using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Runtime.InteropServices;

namespace kchordr
{
    public static class ScanCode 
    {
        public static ushort X = 0x2D;
        public static ushort Y = 0x15;
        public static ushort Escape = 0x01;
    }

    public class Interception
    {
        [UnmanagedFunctionPointer(CallingConvention.Cdecl)]
        public delegate int Predicate(int device);

        [Flags]
        public enum KeyState
        {
            Down = 0x00,
            Up = 0x01,
            E0 = 0x02,
            E1 = 0x04,
            TermsrvSetLED = 0x08,
            TermsrvShadow = 0x10,
            TermsrvVKPacket = 0x20
        }

        [Flags]
        public enum Filter : ushort
        {
            None = 0x0000,
            All = 0xFFFF,
            KeyDown = KeyState.Up,
            KeyUp = KeyState.Up << 1,
            KeyE0 = KeyState.E0 << 1,
            KeyE1 = KeyState.E1 << 1,
            KeyTermsrvSetLED = KeyState.TermsrvSetLED << 1,
            KeyTermsrvShadow = KeyState.TermsrvShadow << 1,
            KeyTermsrvVKPacket = KeyState.TermsrvVKPacket << 1
        }

        [StructLayout(LayoutKind.Sequential)]
        public struct MouseStroke
        {
            public ushort state;
            public ushort flags;
            public short rolling;
            public int x;
            public int y;
            public uint information;
        }

        [StructLayout(LayoutKind.Sequential)]
        public struct KeyStroke
        {
            public ushort code;
            public ushort state;
            public uint information;
        }

        [StructLayout(LayoutKind.Explicit)]
        public struct Stroke
        {
            [FieldOffset(0)] 
            public MouseStroke mouse;
            
            [FieldOffset(0)]
            public KeyStroke key;
        }

        [DllImport("interception.dll", EntryPoint="interception_create_context", CallingConvention=CallingConvention.Cdecl)]
        public static extern IntPtr CreateContext();

        [DllImport("interception.dll", EntryPoint = "interception_destroy_context", CallingConvention = CallingConvention.Cdecl)]
        public static extern void DestroyContext(IntPtr context);

        [DllImport("interception.dll", EntryPoint = "interception_set_filter", CallingConvention = CallingConvention.Cdecl)]
        public static extern void SetFilter(IntPtr context, Predicate predicate, Filter filter);

        [DllImport("interception.dll", EntryPoint = "interception_receive", CallingConvention = CallingConvention.Cdecl)]
        public static extern int Receive(IntPtr context, int device, ref Stroke stroke, uint nstroke);

        [DllImport("interception.dll", EntryPoint = "interception_send", CallingConvention = CallingConvention.Cdecl)]
        public static extern int Send(IntPtr context, int device, ref Stroke stroke, uint nstroke);

        [DllImport("interception.dll", EntryPoint = "interception_is_keyboard", CallingConvention = CallingConvention.Cdecl)]
        public static extern int IsKeyboard(int device);

        [DllImport("interception.dll", EntryPoint = "interception_wait", CallingConvention = CallingConvention.Cdecl)]
        public static extern int Wait(IntPtr context);
    }
    
    class Program
    {
        public static int IsKeyboard(int device)
        {
            return Interception.IsKeyboard(device);
        }

        static void Main(string[] args)
        {
            IntPtr context;
            int device;
            Interception.Stroke stroke = new Interception.Stroke();

            context = Interception.CreateContext();

            Interception.SetFilter(context, IsKeyboard, Interception.Filter.All);

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
