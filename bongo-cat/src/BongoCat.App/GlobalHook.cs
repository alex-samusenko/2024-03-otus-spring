using System.Diagnostics;
using System.Runtime.InteropServices;

namespace BongoCat.App;

public sealed class GlobalHook : IDisposable
{
    private const int WmKeydown = 0x0100;
    private const int WmKeyup = 0x0101;
    private const int WmSyskeydown = 0x0104;
    private const int WmSyskeyup = 0x0105;
    private const int WmLButtonDown = 0x0201;
    private const int WmLButtonUp = 0x0202;
    private const int WmRButtonDown = 0x0204;
    private const int WmRButtonUp = 0x0205;

    private readonly NativeMethods.HookProc _keyboardProc;
    private readonly NativeMethods.HookProc _mouseProc;
    private IntPtr _keyboard;
    private IntPtr _mouse;

    public GlobalHook()
    {
        _keyboardProc = KeyboardProc;
        _mouseProc = MouseProc;
        var moduleName = Process.GetCurrentProcess().MainModule?.ModuleName;
        var module = NativeMethods.GetModuleHandle(moduleName);
        _keyboard = NativeMethods.SetWindowsHookEx(NativeMethods.WhKeyboardLl, _keyboardProc, module, 0);
        _mouse = NativeMethods.SetWindowsHookEx(NativeMethods.WhMouseLl, _mouseProc, module, 0);
    }

    public bool KeyboardAttached => _keyboard != IntPtr.Zero;
    public bool MouseAttached => _mouse != IntPtr.Zero;

    public event Action<int>? KeyDown;
    public event Action<int>? KeyUp;
    public event Action<bool>? LeftButton;
    public event Action<bool>? RightButton;

    public void Dispose()
    {
        if (_keyboard != IntPtr.Zero)
        {
            NativeMethods.UnhookWindowsHookEx(_keyboard);
            _keyboard = IntPtr.Zero;
        }

        if (_mouse != IntPtr.Zero)
        {
            NativeMethods.UnhookWindowsHookEx(_mouse);
            _mouse = IntPtr.Zero;
        }
    }

    private IntPtr KeyboardProc(int nCode, IntPtr wParam, IntPtr lParam)
    {
        if (nCode >= 0)
        {
            try
            {
                var data = Marshal.PtrToStructure<KbdLlHook>(lParam);
                var message = wParam.ToInt32();
                if (message is WmKeydown or WmSyskeydown)
                    KeyDown?.Invoke((int)data.VkCode);
                else if (message is WmKeyup or WmSyskeyup)
                    KeyUp?.Invoke((int)data.VkCode);
            }
            catch
            {
                // Keep the system hook alive even if a listener fails.
            }
        }

        return NativeMethods.CallNextHookEx(_keyboard, nCode, wParam, lParam);
    }

    private IntPtr MouseProc(int nCode, IntPtr wParam, IntPtr lParam)
    {
        if (nCode >= 0)
        {
            try
            {
                switch (wParam.ToInt32())
                {
                    case WmLButtonDown:
                        LeftButton?.Invoke(true);
                        break;
                    case WmLButtonUp:
                        LeftButton?.Invoke(false);
                        break;
                    case WmRButtonDown:
                        RightButton?.Invoke(true);
                        break;
                    case WmRButtonUp:
                        RightButton?.Invoke(false);
                        break;
                }
            }
            catch
            {
                // Keep the system hook alive even if a listener fails.
            }
        }

        return NativeMethods.CallNextHookEx(_mouse, nCode, wParam, lParam);
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct KbdLlHook
    {
        public uint VkCode;
        public uint ScanCode;
        public uint Flags;
        public uint Time;
        public UIntPtr ExtraInfo;
    }
}
