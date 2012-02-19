using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Runtime.InteropServices;

namespace kchordr
{
    class Interception
    {
        delegate int Predicate(int device);

        [Flags]
        enum KeyState
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
        enum Filter : ushort
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
        struct MouseStroke
        {
            ushort state;
            ushort flags;
            short rolling;
            int x;
            int y;
            uint information;
        }

        [StructLayout(LayoutKind.Sequential)]
        struct KeyStroke
        {
            ushort code;
            ushort state;
            uint information;
        }

        [StructLayout(LayoutKind.Explicit)]
        struct Stroke
        {
            [FieldOffset(0)] 
            MouseStroke mouse;
            
            [FieldOffset(0)]
            KeyStroke key;
        }

        [DllImport("interception.dll", EntryPoint="interception_create_context")]
        static extern IntPtr CreateContext();

        [DllImport("interception.dll", EntryPoint = "interception_destroy_context")]
        static extern void DestroyContext(IntPtr context);

        [DllImport("interception.dll", EntryPoint="interception_set_filter")]
        static extern void SetFilter(IntPtr context, Predicate predicate, Filter filter);

        [DllImport("interception.dll", EntryPoint="interception_receive")]
        static extern int Receive(IntPtr context, int device, ref Stroke stroke, uint nstroke);

        [DllImport("interception.dll", EntryPoint="interception_send")]
        static extern int Send(IntPtr context, int device, ref Stroke stroke, uint nstroke);
    }
    
    class Program
    {
        static void Main(string[] args)
        {
        }
    }
}
