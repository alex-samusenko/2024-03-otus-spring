using System.ComponentModel;
using System.Globalization;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using BongoCat.Core;

namespace BongoCat.App;

public partial class SettingsWindow : Window
{
    private readonly AppController _controller;
    private bool _ready;
    private bool _allowClose;

    private TextBlock _live = null!;
    private ProgressBar _level = null!;
    private ComboBox _mics = null!;
    private TextBlock _files = null!;
    private Slider _quiet = null!;
    private Slider _loud = null!;
    private Slider _bangsFrame = null!;
    private Slider _bangsPause = null!;
    private Slider _press = null!;
    private Slider _idle = null!;
    private Slider _blinkFrame = null!;
    private Slider _blinkMin = null!;
    private Slider _blinkMax = null!;
    private Slider _mouth = null!;
    private TextBox _kbX = null!;
    private TextBox _kbY = null!;
    private TextBox _kbW = null!;
    private TextBox _kbH = null!;
    private TextBox _restX = null!;
    private TextBox _restY = null!;
    private TextBox _lift = null!;
    private TextBox _anchorX = null!;
    private TextBox _anchorY = null!;
    private TextBox _winW = null!;
    private TextBox _winH = null!;
    private TextBox _chroma = null!;
    private CheckBox _alpha = null!;
    private CheckBox _click = null!;
    private CheckBox _top = null!;
    private CheckBox _guides = null!;
    private CheckBox _move = null!;

    public SettingsWindow(AppController controller)
    {
        _controller = controller;
        InitializeComponent();
        Build();
        RefreshDevices();
        LoadFromSettings();
        _ready = true;
    }

    public bool AllowClose
    {
        get => _allowClose;
        set => _allowClose = value;
    }

    public void ShowLive(CatPose pose, float level)
    {
        if (!_ready)
            return;

        _level.Value = Math.Clamp(level, 0, 1);
        var input = _controller.InputAttached ? "" : "Перехват клавиатуры или мыши не включился. ";
        _live.Text = input + $"Микрофон {level:0.000}   рот: {MouthName(pose.Mouth)}   лапа: {HandName(pose.Hand)}"
                     + (pose.ActiveKey is null ? "" : $"   клавиша: {pose.ActiveKey}");
    }

    public void RefreshGeometry()
    {
        var wasReady = _ready;
        _ready = false;
        WriteGeometry();
        _files.Text = $"Загружено PNG: {_controller.Assets.LoadedCount}. Холст {_controller.Assets.CanvasWidth:0}×{_controller.Assets.CanvasHeight:0}.";
        _ready = wasReady;
    }

    public void RefreshDevices()
    {
        var selected = _controller.Settings.MicrophoneDevice;
        _mics.Items.Clear();
        foreach (var device in _controller.Microphone.ListDevices())
            _mics.Items.Add(device);

        if (_mics.Items.Count == 0)
        {
            _mics.Items.Add(new MicDevice(-1, "Микрофон не найден"));
            _mics.SelectedIndex = 0;
            return;
        }

        for (var i = 0; i < _mics.Items.Count; i++)
        {
            if (_mics.Items[i] is MicDevice device && device.Index == selected)
            {
                _mics.SelectedIndex = i;
                return;
            }
        }

        _mics.SelectedIndex = 0;
    }

    protected override void OnClosing(CancelEventArgs e)
    {
        if (!_allowClose)
        {
            e.Cancel = true;
            Hide();
            return;
        }

        base.OnClosing(e);
    }

    private void Build()
    {
        Add(Note("Челка, рот, глаза и лапа мыши — PNG одного размера с прозрачным фоном, уже нарисованные на своих местах. Лапа клавиатуры — три картинки: опущена, нажата и поднята. Программа сама ставит нажатую лапу на клавишу, поэтому отдельный кадр на каждую кнопку не нужен. Свой рисунок можно добавить как hand/keys/A.png."));

        _live = Text("Ожидание микрофона…");
        Add(_live);
        _level = new ProgressBar { Minimum = 0, Maximum = 1, Height = 16, Margin = new Thickness(0, 4, 0, 8) };
        Add(_level);

        Add(Heading("Микрофон"));
        var micRow = Row();
        _mics = new ComboBox { MinWidth = 260, Margin = new Thickness(0, 0, 8, 0) };
        _mics.SelectionChanged += (_, _) =>
        {
            if (!_ready || _mics.SelectedItem is not MicDevice device || device.Index < 0)
                return;
            _controller.Settings.MicrophoneDevice = device.Index;
            _controller.RestartMicrophone();
            _controller.Save();
        };
        micRow.Children.Add(_mics);
        micRow.Children.Add(Button("Обновить", RefreshDevices));
        Add(micRow);
        _quiet = Slider("Тихая речь", 0, 0.4, 0.005, value =>
        {
            _controller.Settings.QuietThreshold = (float)value;
            if (_controller.Settings.LoudThreshold < _controller.Settings.QuietThreshold)
                _loud.Value = _controller.Settings.QuietThreshold;
        });
        _loud = Slider("Громкая речь", 0, 0.8, 0.005, value => _controller.Settings.LoudThreshold = (float)value);
        Add(Note("Ниже тихого порога рот закрыт. Между порогами только закрытый и полуоткрытый кадр. Выше громкого сменяются все три кадра."));

        Add(Heading("Челка"));
        _bangsFrame = Slider("Кадр, мс", 100, 2000, 10, value => _controller.Settings.BangsFrameMs = (int)value);
        _bangsPause = Slider("Пауза между движениями, мс", 0, 15000, 100, value => _controller.Settings.BangsPauseMs = (int)value);
        Add(Note("Одно движение — три кадра подряд. Потом пауза, во время неё снова виден первый кадр."));

        Add(Heading("Клавиатура"));
        _press = Slider("Нажатие, мс", 40, 800, 10, value => _controller.Settings.KeyPressMs = (int)value);
        _idle = Slider("До опущенной руки, мс", 100, 4000, 20, value => _controller.Settings.KeyboardIdleMs = (int)value);
        Add(Note("После нажатия лапа лежит на клавише, затем поднимается. Если новых клавиш нет, рука опускается. При быстрой печати уменьшите «Нажатие» до 100–150 мс, иначе лапа не успеет подняться между буквами."));

        Add(Heading("Глаза и рот"));
        _blinkFrame = Slider("Кадр моргания, мс", 30, 200, 5, value => _controller.Settings.BlinkFrameMs = (int)value);
        _blinkMin = Slider("Пауза минимум, мс", 200, 10000, 50, value => _controller.Settings.BlinkMinMs = (int)value);
        _blinkMax = Slider("Пауза максимум, мс", 400, 15000, 50, value => _controller.Settings.BlinkMaxMs = (int)value);
        _mouth = Slider("Кадр рта, мс", 40, 400, 10, value => _controller.Settings.MouthFrameMs = (int)value);
        Add(Note("Моргание — полузакрытый, закрытый, полузакрытый. Пауза каждый раз случайная, в заданных пределах. Иногда моргание двойное."));

        Add(Heading("Куда ставить лапу"));
        Add(Note("Координаты в пикселях картинки тела. Розовая рамка и точки — сетка клавиш, синяя точка — опущенная рука. Точка привязки лапы — где на картинке лапы находится палец."));
        _kbX = Box("Клавиатура X");
        _kbY = Box("Клавиатура Y");
        _kbW = Box("Клавиатура ширина");
        _kbH = Box("Клавиатура высота");
        _restX = Box("Опущенная рука X");
        _restY = Box("Опущенная рука Y");
        _lift = Box("Подъём между клавишами");
        _anchorX = Box("Палец по горизонтали, 0–1");
        _anchorY = Box("Палец по вертикали, 0–1");
        var geometryButtons = Row();
        geometryButtons.Children.Add(Button("Применить координаты", ApplyGeometry));
        geometryButtons.Children.Add(Button("Сетка клавиш", () => _guides.IsChecked = true));
        Add(geometryButtons);

        Add(Heading("Окно для OBS"));
        _files = Text("");
        Add(_files);
        _winW = Box("Ширина окна");
        _winH = Box("Высота окна");
        _chroma = Box("Цвет хромакея, #RRGGBB");
        _alpha = Check("Настоящая прозрачность вместо хромакея", value => _controller.SetPerPixelAlpha(value));
        _click = Check("Клики проходят сквозь кота", value =>
        {
            _controller.Settings.ClickThrough = value;
            _controller.ApplyLiveSettings();
        });
        _top = Check("Поверх всех окон", value =>
        {
            _controller.Settings.Topmost = value;
            _controller.ApplyLiveSettings();
        });
        _guides = Check("Показать сетку клавиш", value =>
        {
            _controller.Settings.ShowKeyGuides = value;
            _controller.ApplyLiveSettings();
        });
        _move = Check("Двигать и менять размер (пока включено, клики не проходят сквозь)", value => _controller.SetMoveMode(value));

        var buttons = Row();
        buttons.Children.Add(Button("Перезагрузить картинки", () =>
        {
            _controller.ReloadAssets();
            RefreshGeometry();
        }));
        buttons.Children.Add(Button("Открыть папку assets", _controller.OpenAssetsFolder));
        buttons.Children.Add(Button("Сохранить", () =>
        {
            ApplyGeometry();
            ApplyWindowSize();
            _controller.Save();
        }));
        buttons.Children.Add(Button("Выход", _controller.Shutdown));
        Add(buttons);
        Add(Note("В OBS добавьте источник «Захват окна» и выберите Bongo Cat. На зелёный фон повесьте фильтр «Хромакей». Цвет по умолчанию #00FF00."));
    }

    private void LoadFromSettings()
    {
        var settings = _controller.Settings;
        _quiet.Value = settings.QuietThreshold;
        _loud.Value = settings.LoudThreshold;
        _bangsFrame.Value = settings.BangsFrameMs;
        _bangsPause.Value = settings.BangsPauseMs;
        _press.Value = settings.KeyPressMs;
        _idle.Value = settings.KeyboardIdleMs;
        _blinkFrame.Value = settings.BlinkFrameMs;
        _blinkMin.Value = settings.BlinkMinMs;
        _blinkMax.Value = settings.BlinkMaxMs;
        _mouth.Value = settings.MouthFrameMs;
        _alpha.IsChecked = settings.PerPixelAlpha;
        _click.IsChecked = settings.ClickThrough;
        _top.IsChecked = settings.Topmost;
        _guides.IsChecked = settings.ShowKeyGuides;
        _move.IsChecked = _controller.MoveMode;
        _chroma.Text = settings.ChromaHex;
        _winW.Text = settings.WindowWidth.ToString("0", CultureInfo.InvariantCulture);
        _winH.Text = settings.WindowHeight.ToString("0", CultureInfo.InvariantCulture);
        WriteGeometry();
        _files.Text = $"Загружено PNG: {_controller.Assets.LoadedCount}. Холст {_controller.Assets.CanvasWidth:0}×{_controller.Assets.CanvasHeight:0}.";
    }

    private void WriteGeometry()
    {
        var settings = _controller.Settings;
        var width = CanvasWidth;
        var height = CanvasHeight;
        _kbX.Text = Px(settings.KeyboardX * width);
        _kbY.Text = Px(settings.KeyboardY * height);
        _kbW.Text = Px(settings.KeyboardW * width);
        _kbH.Text = Px(settings.KeyboardH * height);
        _restX.Text = Px(settings.HandRestX * width);
        _restY.Text = Px(settings.HandRestY * height);
        _lift.Text = Px(settings.RaisedLift * height);
        _anchorX.Text = settings.HandAnchorX.ToString("0.00", CultureInfo.InvariantCulture);
        _anchorY.Text = settings.HandAnchorY.ToString("0.00", CultureInfo.InvariantCulture);
    }

    private void ApplyGeometry()
    {
        var settings = _controller.Settings;
        var width = CanvasWidth;
        var height = CanvasHeight;
        settings.KeyboardX = Fraction(_kbX, width);
        settings.KeyboardY = Fraction(_kbY, height);
        settings.KeyboardW = Math.Max(0.05, Fraction(_kbW, width));
        settings.KeyboardH = Math.Max(0.05, Fraction(_kbH, height));
        settings.HandRestX = Fraction(_restX, width);
        settings.HandRestY = Fraction(_restY, height);
        settings.RaisedLift = Math.Max(0, Fraction(_lift, height));
        if (TryNumber(_anchorX.Text, out var anchorX))
            settings.HandAnchorX = Math.Clamp(anchorX, 0, 1);
        if (TryNumber(_anchorY.Text, out var anchorY))
            settings.HandAnchorY = Math.Clamp(anchorY, 0, 1);
        settings.ChromaHex = string.IsNullOrWhiteSpace(_chroma.Text) ? "#00FF00" : _chroma.Text.Trim();
        ApplyWindowSize();
        _controller.ApplyLiveSettings();
    }

    private void ApplyWindowSize()
    {
        if (TryNumber(_winW.Text, out var width))
            _controller.Overlay.Width = Math.Clamp(width, 240, 3840);
        if (TryNumber(_winH.Text, out var height))
            _controller.Overlay.Height = Math.Clamp(height, 180, 2160);
    }

    private double CanvasWidth => Math.Max(1, _controller.Assets.CanvasWidth);
    private double CanvasHeight => Math.Max(1, _controller.Assets.CanvasHeight);

    private Slider Slider(string label, double min, double max, double step, Action<double> apply)
    {
        var valueText = Text("");
        var slider = new Slider
        {
            Minimum = min,
            Maximum = max,
            TickFrequency = step,
            IsSnapToTickEnabled = true,
            Margin = new Thickness(0, 2, 0, 8)
        };
        slider.ValueChanged += (_, _) =>
        {
            valueText.Text = $"{label}: {slider.Value:0.###}";
            if (!_ready)
                return;
            apply(slider.Value);
            _controller.ApplyLiveSettings();
        };
        valueText.Text = label;
        Add(valueText);
        Add(slider);
        return slider;
    }

    private TextBox Box(string label)
    {
        Add(Text(label));
        var box = new TextBox
        {
            Margin = new Thickness(0, 2, 0, 8),
            Padding = new Thickness(6, 4, 6, 4),
            Background = Brushes.White,
            Foreground = Brushes.Black
        };
        box.LostFocus += (_, _) =>
        {
            if (_ready)
                ApplyGeometry();
        };
        Add(box);
        return box;
    }

    private CheckBox Check(string label, Action<bool> apply)
    {
        var box = new CheckBox
        {
            Content = label,
            Margin = new Thickness(0, 4, 0, 4),
            Foreground = Foreground
        };
        box.Checked += (_, _) =>
        {
            if (_ready)
                apply(true);
        };
        box.Unchecked += (_, _) =>
        {
            if (_ready)
                apply(false);
        };
        Add(box);
        return box;
    }

    private Button Button(string label, Action click)
    {
        var button = new Button
        {
            Content = label,
            Margin = new Thickness(0, 0, 8, 8),
            Padding = new Thickness(10, 6, 10, 6)
        };
        button.Click += (_, _) => click();
        return button;
    }

    private static StackPanel Row() => new()
    {
        Orientation = Orientation.Horizontal,
        Margin = new Thickness(0, 0, 0, 8)
    };

    private static TextBlock Heading(string text) => new()
    {
        Text = text,
        FontSize = 16,
        FontWeight = FontWeights.SemiBold,
        Margin = new Thickness(0, 14, 0, 6)
    };

    private static TextBlock Note(string text) => new()
    {
        Text = text,
        TextWrapping = TextWrapping.Wrap,
        Opacity = 0.82,
        Margin = new Thickness(0, 0, 0, 8)
    };

    private static TextBlock Text(string text) => new()
    {
        Text = text,
        TextWrapping = TextWrapping.Wrap,
        Margin = new Thickness(0, 0, 0, 2)
    };

    private void Add(UIElement element) => Panel.Children.Add(element);

    private static string Px(double value) => Math.Round(value).ToString(CultureInfo.InvariantCulture);

    private static double Fraction(TextBox box, double canvas)
    {
        return TryNumber(box.Text, out var pixels) ? pixels / canvas : 0;
    }

    private static bool TryNumber(string text, out double value)
    {
        text = text.Trim().Replace(',', '.');
        return double.TryParse(text, NumberStyles.Float, CultureInfo.InvariantCulture, out value);
    }

    private static string MouthName(MouthFrame frame) => frame switch
    {
        MouthFrame.Half => "полуоткрыт",
        MouthFrame.Open => "открыт",
        _ => "закрыт"
    };

    private static string HandName(HandPose pose) => pose switch
    {
        HandPose.Press => "на клавише",
        HandPose.Raised => "поднята",
        _ => "опущена"
    };
}
