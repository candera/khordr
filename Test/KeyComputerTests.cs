using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using Xunit;

namespace kchordr.Test
{
    public class KeyComputerTests
    {
        private readonly KeyComputer _computer;

        private Interception.Stroke KeyStroke(ushort code, Interception.KeyState state)
        {
            Interception.Stroke stroke = new Interception.Stroke();

            stroke.key.code = code;
            stroke.key.state = (ushort) state;

            return stroke;
        }

        public KeyComputerTests()
        {
            _computer = new KeyComputer();
        }

        [Fact]
        public void XDown()
        {
            _computer.Receive(KeyStroke(ScanCode.X, Interception.KeyState.Down));

            Assert.Equal(1, _computer.Transmissions.Count);
            Assert.Equal(KeyStroke(ScanCode.X, Interception.KeyState.Down), _computer.Transmissions.First());
        }

        [Fact]
        public void JDown()
        {
            _computer.Receive(KeyStroke(ScanCode.J, Interception.KeyState.Down));
            Assert.Equal(0, _computer.Transmissions.Count);
        }

        [Fact]
        public void JDownUp()
        {
            _computer.Receive(KeyStroke(ScanCode.J, Interception.KeyState.Down));
            _computer.Receive(KeyStroke(ScanCode.J, Interception.KeyState.Up));
            Assert.Equal(2, _computer.Transmissions.Count);
            Assert.Equal(KeyStroke(ScanCode.J, Interception.KeyState.Down), _computer.Transmissions.ElementAt(0));
            Assert.Equal(KeyStroke(ScanCode.J, Interception.KeyState.Up), _computer.Transmissions.ElementAt(1));
        }
    }
}
