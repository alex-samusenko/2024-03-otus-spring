namespace BongoCat.Core;

public sealed class AnimationTuning
{
    public TimeSpan BangsFrameDuration { get; set; } = TimeSpan.FromMilliseconds(500);

    public TimeSpan BangsPause { get; set; } = TimeSpan.FromSeconds(5);

    public TimeSpan KeyPressDuration { get; set; } = TimeSpan.FromMilliseconds(300);

    public TimeSpan KeyboardIdleDelay { get; set; } = TimeSpan.FromMilliseconds(1000);

    public TimeSpan BlinkFrameDuration { get; set; } = TimeSpan.FromMilliseconds(70);

    public TimeSpan BlinkGapMin { get; set; } = TimeSpan.FromMilliseconds(2000);

    public TimeSpan BlinkGapMax { get; set; } = TimeSpan.FromMilliseconds(6200);

    public TimeSpan DoubleBlinkGap { get; set; } = TimeSpan.FromMilliseconds(140);

    public double DoubleBlinkChance { get; set; } = 0.22;

    public TimeSpan MouthFrameDuration { get; set; } = TimeSpan.FromMilliseconds(110);

    public float QuietThreshold { get; set; } = 0.035f;

    public float LoudThreshold { get; set; } = 0.14f;
}
