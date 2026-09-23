using System.Diagnostics;
using System.IO;
using System.Windows.Input;
using System.Windows.Threading;
using BongoCat.Core;

namespace BongoCat.App;

public sealed class AppController : IDisposable
{
    private readonly string _settingsPath;
    private readonly DispatcherTimer _timer;
    private readonly Stopwatch _clock = new();
    private TimeSpan _lastTick;
    private GlobalHook? _hook;
    private System.Windows.Forms.NotifyIcon? _tray;
    private bool _moveMode;
    private bool _recreating;
    private bool _shuttingDown;
    private bool _started;

    public AppController()
    {
        var root = AppContext.BaseDirectory;
        _settingsPath = SettingsStore.PathFor(root);
        AssetsRoot = Path.Combine(root, "assets");
        Settings = SettingsStore.Load(_settingsPath);
        Tuning = new AnimationTuning();
        Settings.CopyTo(Tuning);
        Animator = new CatAnimator(Tuning);
        Assets = new AssetLibrary();
        Microphone = new MicrophoneMonitor();
        Assets.Load(AssetsRoot);
        Overlay = CreateOverlay();
        SettingsWindow = new SettingsWindow(this);
        _timer = new DispatcherTimer { Interval = TimeSpan.FromMilliseconds(16) };
        _timer.Tick += OnTick;
    }

    public UserSettings Settings { get; }
    public AnimationTuning Tuning { get; }
    public CatAnimator Animator { get; }
    public AssetLibrary Assets { get; }
    public MicrophoneMonitor Microphone { get; }
    public OverlayWindow Overlay { get; private set; }
    public SettingsWindow SettingsWindow { get; }
    public string AssetsRoot { get; }
    public bool MoveMode => _moveMode;

    public bool InputAttached => _hook is { KeyboardAttached: true, MouseAttached: true };

    public void Start()
    {
        if (_started)
            return;

        _started = true;
        Overlay.Place(Settings);
        Overlay.ApplyAppearance(Settings, _moveMode);
        Overlay.SetGuides(Settings.ShowKeyGuides);
        Overlay.BoundsChanged += SaveBounds;
        Overlay.Closed += OnOverlayClosed;
        Overlay.Show();
        SettingsWindow.Show();
        AttachHook();
        CreateTray();
        TryStartMicrophone();
        _clock.Start();
        _timer.Start();
    }

    public void ReloadAssets()
    {
        Assets.Load(AssetsRoot);
        Overlay.SetGuides(Settings.ShowKeyGuides);
        SettingsWindow.RefreshGeometry();
    }

    public void ApplyLiveSettings()
    {
        Settings.CopyTo(Tuning);
        Overlay.ApplyAppearance(Settings, _moveMode);
        Overlay.SetGuides(Settings.ShowKeyGuides);
        Save();
    }

    public void SetMoveMode(bool enabled)
    {
        _moveMode = enabled;
        Overlay.ApplyAppearance(Settings, _moveMode);
    }

    public void SetPerPixelAlpha(bool enabled)
    {
        if (Settings.PerPixelAlpha == enabled)
            return;

        Settings.PerPixelAlpha = enabled;
        Save();
        RecreateOverlay();
    }

    public void RestartMicrophone() => TryStartMicrophone();

    public void OpenAssetsFolder()
    {
        Directory.CreateDirectory(AssetsRoot);
        Process.Start(new ProcessStartInfo(AssetsRoot) { UseShellExecute = true });
    }

    public void Save() => SettingsStore.Save(_settingsPath, Settings);

    public void Shutdown()
    {
        if (_shuttingDown)
            return;

        _shuttingDown = true;
        Save();
        SettingsWindow.AllowClose = true;
        Dispose();
        System.Windows.Application.Current?.Shutdown();
    }

    public void Dispose()
    {
        _timer.Stop();
        _hook?.Dispose();
        _hook = null;
        Microphone.Dispose();
        if (_tray is not null)
        {
            _tray.Visible = false;
            _tray.Dispose();
            _tray = null;
        }
    }

    private void OnTick(object? sender, EventArgs e)
    {
        var now = _clock.Elapsed;
        var delta = now - _lastTick;
        _lastTick = now;
        if (delta > TimeSpan.FromMilliseconds(80))
            delta = TimeSpan.FromMilliseconds(80);

        Animator.SetLevel(Microphone.Level);
        Animator.Advance(delta);
        Overlay.Render(Animator.Pose, Assets, Settings);
        SettingsWindow.ShowLive(Animator.Pose, Microphone.Level);
    }

    private void AttachHook()
    {
        try
        {
            _hook = new GlobalHook();
            _hook.KeyDown += vk => SendKey(vk, down: true);
            _hook.KeyUp += vk => SendKey(vk, down: false);
            _hook.LeftButton += down => SendMouse(PointerButton.Left, down);
            _hook.RightButton += down => SendMouse(PointerButton.Right, down);
        }
        catch
        {
            _hook = null;
        }
    }

    private void SendKey(int virtualKey, bool down)
    {
        var key = KeyInterop.KeyFromVirtualKey(virtualKey);
        if (key == Key.None)
            return;

        var name = key.ToString();
        if (down)
            Animator.KeyDown(name);
        else
            Animator.KeyUp(name);
    }

    private void SendMouse(PointerButton button, bool down)
    {
        if (down)
            Animator.MouseDown(button);
        else
            Animator.MouseUp(button);
    }

    private void TryStartMicrophone()
    {
        try
        {
            Microphone.Start(Settings.MicrophoneDevice);
        }
        catch
        {
            // The overlay still works without a microphone; the mouth stays closed.
        }
    }

    private void SaveBounds()
    {
        if (!Overlay.IsLoaded)
            return;

        Settings.WindowLeft = Overlay.Left;
        Settings.WindowTop = Overlay.Top;
        Settings.WindowWidth = Overlay.Width;
        Settings.WindowHeight = Overlay.Height;
        Save();
    }

    private void OnOverlayClosed(object? sender, EventArgs e)
    {
        if (!_recreating)
            Shutdown();
    }

    private void RecreateOverlay()
    {
        _recreating = true;
        _timer.Stop();
        var previous = Overlay;
        previous.BoundsChanged -= SaveBounds;
        previous.Closed -= OnOverlayClosed;
        previous.Close();

        Overlay = CreateOverlay();
        Overlay.Place(Settings);
        Overlay.ApplyAppearance(Settings, _moveMode);
        Overlay.SetGuides(Settings.ShowKeyGuides);
        Overlay.BoundsChanged += SaveBounds;
        Overlay.Closed += OnOverlayClosed;
        Overlay.Show();
        _recreating = false;
        _timer.Start();
    }

    private OverlayWindow CreateOverlay() => new(Settings.PerPixelAlpha, ParseColor(Settings.ChromaHex));

    private void CreateTray()
    {
        var menu = new System.Windows.Forms.ContextMenuStrip();
        menu.Items.Add("Настройки", null, (_, _) => ShowSettings());
        menu.Items.Add("Выход", null, (_, _) => Shutdown());

        _tray = new System.Windows.Forms.NotifyIcon
        {
            Icon = System.Drawing.SystemIcons.Application,
            Visible = true,
            Text = "Bongo Cat",
            ContextMenuStrip = menu
        };
        _tray.DoubleClick += (_, _) => ShowSettings();
    }

    private void ShowSettings()
    {
        SettingsWindow.Show();
        SettingsWindow.WindowState = System.Windows.WindowState.Normal;
        SettingsWindow.Activate();
    }

    private static System.Windows.Media.Color ParseColor(string hex)
    {
        try
        {
            return (System.Windows.Media.Color)System.Windows.Media.ColorConverter.ConvertFromString(hex)!;
        }
        catch
        {
            return System.Windows.Media.Colors.Lime;
        }
    }
}
