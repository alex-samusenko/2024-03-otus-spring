using System.IO;
using System.Text.Json;
using BongoCat.Core;

namespace BongoCat.App;

public sealed class UserSettings
{
    public double WindowLeft { get; set; } = 80;
    public double WindowTop { get; set; } = 80;
    public double WindowWidth { get; set; } = 960;
    public double WindowHeight { get; set; } = 640;
    public bool ClickThrough { get; set; } = true;
    public bool Topmost { get; set; } = true;
    public bool PerPixelAlpha { get; set; }
    public string ChromaHex { get; set; } = "#00FF00";
    public int MicrophoneDevice { get; set; }
    public float QuietThreshold { get; set; } = 0.035f;
    public float LoudThreshold { get; set; } = 0.14f;
    public int BangsFrameMs { get; set; } = 500;
    public int BangsPauseMs { get; set; } = 5000;
    public int KeyPressMs { get; set; } = 300;
    public int KeyboardIdleMs { get; set; } = 1000;
    public int BlinkFrameMs { get; set; } = 70;
    public int BlinkMinMs { get; set; } = 2000;
    public int BlinkMaxMs { get; set; } = 6200;
    public int MouthFrameMs { get; set; } = 110;
    public double DoubleBlinkChance { get; set; } = 0.22;
    public double KeyboardX { get; set; } = 180.0 / 960.0;
    public double KeyboardY { get; set; } = 420.0 / 640.0;
    public double KeyboardW { get; set; } = 580.0 / 960.0;
    public double KeyboardH { get; set; } = 180.0 / 640.0;
    public double HandRestX { get; set; } = 150.0 / 960.0;
    public double HandRestY { get; set; } = 530.0 / 640.0;
    public double RaisedLift { get; set; } = 48.0 / 640.0;
    public double HandAnchorX { get; set; } = 0.50;
    public double HandAnchorY { get; set; } = 0.92;
    public bool ShowKeyGuides { get; set; }

    public void CopyTo(AnimationTuning tuning)
    {
        tuning.BangsFrameDuration = TimeSpan.FromMilliseconds(Math.Max(20, BangsFrameMs));
        tuning.BangsPause = TimeSpan.FromMilliseconds(Math.Max(0, BangsPauseMs));
        tuning.KeyPressDuration = TimeSpan.FromMilliseconds(Math.Max(40, KeyPressMs));
        tuning.KeyboardIdleDelay = TimeSpan.FromMilliseconds(Math.Max(80, KeyboardIdleMs));
        tuning.BlinkFrameDuration = TimeSpan.FromMilliseconds(Math.Max(15, BlinkFrameMs));
        tuning.BlinkGapMin = TimeSpan.FromMilliseconds(Math.Max(200, BlinkMinMs));
        tuning.BlinkGapMax = TimeSpan.FromMilliseconds(Math.Max(BlinkMinMs, BlinkMaxMs));
        tuning.DoubleBlinkChance = Math.Clamp(DoubleBlinkChance, 0, 1);
        tuning.MouthFrameDuration = TimeSpan.FromMilliseconds(Math.Max(40, MouthFrameMs));
        tuning.QuietThreshold = Math.Clamp(QuietThreshold, 0f, 1f);
        tuning.LoudThreshold = Math.Clamp(LoudThreshold, QuietThreshold, 1f);
    }
}

public static class SettingsStore
{
    private static readonly JsonSerializerOptions JsonOptions = new() { WriteIndented = true };

    public static string PathFor(string baseDirectory) => Path.Combine(baseDirectory, "settings.json");

    public static UserSettings Load(string path)
    {
        try
        {
            if (!File.Exists(path))
                return new UserSettings();

            return JsonSerializer.Deserialize<UserSettings>(File.ReadAllText(path)) ?? new UserSettings();
        }
        catch
        {
            return new UserSettings();
        }
    }

    public static void Save(string path, UserSettings settings)
    {
        var directory = Path.GetDirectoryName(path);
        if (!string.IsNullOrEmpty(directory))
            Directory.CreateDirectory(directory);

        File.WriteAllText(path, JsonSerializer.Serialize(settings, JsonOptions));
    }
}
