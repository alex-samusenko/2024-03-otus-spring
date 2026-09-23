using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Interop;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Shapes;
using BongoCat.Core;

namespace BongoCat.App;

public partial class OverlayWindow : Window
{
    private readonly bool _perPixelAlpha;
    private bool _clickThrough = true;
    private bool _moveMode;
    private bool _guides;
    private bool _resizing;
    private Point _resizeStart;
    private double _resizeWidth;
    private double _resizeHeight;
    private int _guideVersion = -1;

    public OverlayWindow(bool perPixelAlpha, Color chroma)
    {
        _perPixelAlpha = perPixelAlpha;
        if (perPixelAlpha)
            AllowsTransparency = true;

        InitializeComponent();
        Background = perPixelAlpha ? Brushes.Transparent : new SolidColorBrush(chroma);
        MinWidth = 240;
        MinHeight = 180;
    }

    public event Action? BoundsChanged;

    public void Place(UserSettings settings)
    {
        Left = settings.WindowLeft;
        Top = settings.WindowTop;
        Width = Math.Max(MinWidth, settings.WindowWidth);
        Height = Math.Max(MinHeight, settings.WindowHeight);
        Topmost = settings.Topmost;
    }

    public void ApplyAppearance(UserSettings settings, bool moveMode)
    {
        Topmost = settings.Topmost;
        _clickThrough = settings.ClickThrough;
        _moveMode = moveMode;
        Cursor = moveMode ? Cursors.SizeAll : Cursors.Arrow;
        ResizeThumb.Visibility = moveMode ? Visibility.Visible : Visibility.Collapsed;
        if (!_perPixelAlpha)
            Background = new SolidColorBrush(ParseColor(settings.ChromaHex));
        ApplyHitTest();
    }

    public void Render(CatPose pose, AssetLibrary assets, UserSettings settings)
    {
        if (Math.Abs(Stage.Width - assets.CanvasWidth) > 0.5 || Math.Abs(Stage.Height - assets.CanvasHeight) > 0.5)
        {
            Stage.Width = assets.CanvasWidth;
            Stage.Height = assets.CanvasHeight;
            _guideVersion = -1;
        }

        ShowFull(BodyImage, assets.Body);
        ShowFull(MouthImage, MouthSource(pose.Mouth, assets));
        ShowFull(EyesImage, EyeSource(pose.EyeFrame, assets));
        ShowFull(BangsImage, assets.Bangs[Math.Clamp(pose.BangsFrame, 0, 2)]);
        ShowFull(MouseImage, MouseSource(pose.Mouse, assets));
        PlaceHand(pose, assets, settings);
        EmptyHint.Visibility = assets.Body is null ? Visibility.Visible : Visibility.Collapsed;

        var version = HashCode.Combine(
            HashCode.Combine(_guides, settings.KeyboardX, settings.KeyboardY, settings.KeyboardW, settings.KeyboardH),
            HashCode.Combine(settings.HandRestX, settings.HandRestY, assets.CanvasWidth, assets.CanvasHeight));
        if (version != _guideVersion)
        {
            _guideVersion = version;
            RebuildGuides(settings);
        }
    }

    public void SetGuides(bool show)
    {
        _guides = show;
        _guideVersion = -1;
    }

    protected override void OnSourceInitialized(EventArgs e)
    {
        base.OnSourceInitialized(e);
        ApplyHitTest();
    }

    protected override void OnMouseLeftButtonDown(MouseButtonEventArgs e)
    {
        if (_moveMode && !ReferenceEquals(e.Source, ResizeThumb))
            DragMove();
        base.OnMouseLeftButtonDown(e);
    }

    private void ResizeThumb_OnMouseLeftButtonDown(object sender, MouseButtonEventArgs e)
    {
        if (!_moveMode)
            return;

        _resizing = true;
        _resizeStart = PointToScreen(e.GetPosition(this));
        _resizeWidth = Width;
        _resizeHeight = Height;
        ResizeThumb.CaptureMouse();
        e.Handled = true;
    }

    private void ResizeThumb_OnMouseMove(object sender, MouseEventArgs e)
    {
        if (!_resizing)
            return;

        var screen = PointToScreen(e.GetPosition(this));
        var source = PresentationSource.FromVisual(this);
        var scaleX = source?.CompositionTarget?.TransformToDevice.M11 ?? 1;
        var scaleY = source?.CompositionTarget?.TransformToDevice.M22 ?? 1;
        if (scaleX <= 0)
            scaleX = 1;
        if (scaleY <= 0)
            scaleY = 1;

        Width = Math.Max(MinWidth, _resizeWidth + (screen.X - _resizeStart.X) / scaleX);
        Height = Math.Max(MinHeight, _resizeHeight + (screen.Y - _resizeStart.Y) / scaleY);
        e.Handled = true;
    }

    private void ResizeThumb_OnMouseLeftButtonUp(object sender, MouseButtonEventArgs e)
    {
        if (!_resizing)
            return;

        _resizing = false;
        ResizeThumb.ReleaseMouseCapture();
        BoundsChanged?.Invoke();
        e.Handled = true;
    }

    protected override void OnMouseLeftButtonUp(MouseButtonEventArgs e)
    {
        base.OnMouseLeftButtonUp(e);
        if (_moveMode)
            BoundsChanged?.Invoke();
    }

    protected override void OnLocationChanged(EventArgs e)
    {
        base.OnLocationChanged(e);
        if (IsLoaded)
            BoundsChanged?.Invoke();
    }

    protected override void OnRenderSizeChanged(SizeChangedInfo sizeInfo)
    {
        base.OnRenderSizeChanged(sizeInfo);
        if (IsLoaded)
            BoundsChanged?.Invoke();
    }

    private void ApplyHitTest()
    {
        var hwnd = new WindowInteropHelper(this).Handle;
        if (hwnd == IntPtr.Zero)
            return;

        var style = NativeMethods.GetWindowLongPtr(hwnd, NativeMethods.GwlExstyle).ToInt64();
        if (_clickThrough && !_moveMode)
            style |= NativeMethods.WsExTransparent;
        else
            style &= ~NativeMethods.WsExTransparent;

        NativeMethods.SetWindowLongPtr(hwnd, NativeMethods.GwlExstyle, new IntPtr(style));
    }

    private void PlaceHand(CatPose pose, AssetLibrary assets, UserSettings settings)
    {
        BitmapImage? source = pose.Hand switch
        {
            HandPose.Idle => assets.HandIdle,
            HandPose.Raised => assets.HandRaised ?? assets.HandPress,
            _ => assets.PressFor(pose.ActiveKey)
        };

        if (source is null)
        {
            HandImage.Source = null;
            HandImage.Visibility = Visibility.Collapsed;
            return;
        }

        var spot = HandPlacementMath.Resolve(
            pose,
            Stage.Width,
            Stage.Height,
            settings.KeyboardX,
            settings.KeyboardY,
            settings.KeyboardW,
            settings.KeyboardH,
            settings.HandRestX,
            settings.HandRestY,
            settings.RaisedLift);

        HandImage.Visibility = Visibility.Visible;
        if (!ReferenceEquals(HandImage.Source, source))
            HandImage.Source = source;

        var fullCanvas = Math.Abs(source.Width - Stage.Width) < 2 && Math.Abs(source.Height - Stage.Height) < 2;
        if (fullCanvas)
        {
            HandImage.Width = Stage.Width;
            HandImage.Height = Stage.Height;
            Canvas.SetLeft(HandImage, 0);
            Canvas.SetTop(HandImage, 0);
            return;
        }

        HandImage.Width = source.Width;
        HandImage.Height = source.Height;
        Canvas.SetLeft(HandImage, spot.X - source.Width * settings.HandAnchorX);
        Canvas.SetTop(HandImage, spot.Y - source.Height * settings.HandAnchorY);
    }

    private void ShowFull(Image image, BitmapImage? source)
    {
        if (source is null)
        {
            image.Source = null;
            image.Visibility = Visibility.Collapsed;
            return;
        }

        image.Visibility = Visibility.Visible;
        image.Width = Stage.Width;
        image.Height = Stage.Height;
        Canvas.SetLeft(image, 0);
        Canvas.SetTop(image, 0);
        if (!ReferenceEquals(image.Source, source))
            image.Source = source;
    }

    private void RebuildGuides(UserSettings settings)
    {
        Guides.Children.Clear();
        if (!_guides)
            return;

        var width = Stage.Width;
        var height = Stage.Height;
        var frame = new Rectangle
        {
            Width = Math.Max(1, settings.KeyboardW * width),
            Height = Math.Max(1, settings.KeyboardH * height),
            Stroke = Brushes.DeepPink,
            StrokeThickness = 2
        };
        Canvas.SetLeft(frame, settings.KeyboardX * width);
        Canvas.SetTop(frame, settings.KeyboardY * height);
        Guides.Children.Add(frame);

        var seen = new HashSet<(int X, int Y)>();
        foreach (var key in KeyboardLayout.Keys)
        {
            if (!KeyboardLayout.TryGet(key, out var anchor))
                continue;

            var x = (settings.KeyboardX + anchor.X * settings.KeyboardW) * width;
            var y = (settings.KeyboardY + anchor.Y * settings.KeyboardH) * height;
            var point = ((int)Math.Round(x), (int)Math.Round(y));
            if (!seen.Add(point))
                continue;

            var dot = new Ellipse
            {
                Width = 6,
                Height = 6,
                Fill = Brushes.DeepPink
            };
            Canvas.SetLeft(dot, x - 3);
            Canvas.SetTop(dot, y - 3);
            Guides.Children.Add(dot);
        }

        var rest = new Ellipse
        {
            Width = 10,
            Height = 10,
            Fill = Brushes.DodgerBlue
        };
        Canvas.SetLeft(rest, settings.HandRestX * width - 5);
        Canvas.SetTop(rest, settings.HandRestY * height - 5);
        Guides.Children.Add(rest);
    }

    private static BitmapImage? MouthSource(MouthFrame frame, AssetLibrary assets) => frame switch
    {
        MouthFrame.Half => assets.MouthHalf,
        MouthFrame.Open => assets.MouthOpen,
        _ => assets.MouthClosed
    };

    private static BitmapImage? EyeSource(int frame, AssetLibrary assets) => frame switch
    {
        1 => assets.EyeHalf,
        2 => assets.EyeClosed,
        _ => assets.EyeOpen
    };

    private static BitmapImage? MouseSource(PointerButton button, AssetLibrary assets) => button switch
    {
        PointerButton.Left => assets.MouseLeft,
        PointerButton.Right => assets.MouseRight,
        _ => assets.MouseIdle
    };

    private static Color ParseColor(string hex)
    {
        try
        {
            return (Color)ColorConverter.ConvertFromString(hex)!;
        }
        catch
        {
            return Colors.Lime;
        }
    }
}
