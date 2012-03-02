using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace kchordr
{
    public class KeyComputer
    {
        private readonly Queue<Interception.Stroke> _transmissions = new Queue<Interception.Stroke>();

        public Queue<Interception.Stroke> Transmissions
        {
            get { return _transmissions; }
        }

        public void Receive(Interception.Stroke stroke)
        {
            if (stroke.key.code == ScanCode.J && stroke.key.state == (ushort) Interception.KeyState.Up)
            {

                _transmissions.Enqueue(
            }
            _transmissions.Enqueue(stroke);
        }
    }
}
