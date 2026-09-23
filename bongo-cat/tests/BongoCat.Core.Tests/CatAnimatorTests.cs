using BongoCat.Core;

namespace BongoCat.Core.Tests;

public class CatAnimatorTests
{
    [Fact]
    public void Bangs_play_three_frames_then_wait_five_seconds()
    {
        var cat = Create(tuning =>
        {
            tuning.BangsFrameDuration = TimeSpan.FromMilliseconds(500);
            tuning.BangsPause = TimeSpan.FromSeconds(5);
        });

        Assert.Equal(0, cat.Pose.BangsFrame);
        cat.Advance(TimeSpan.FromMilliseconds(499));
        Assert.Equal(0, cat.Pose.BangsFrame);
        cat.Advance(TimeSpan.FromMilliseconds(1));
        Assert.Equal(1, cat.Pose.BangsFrame);
        cat.Advance(TimeSpan.FromMilliseconds(500));
        Assert.Equal(2, cat.Pose.BangsFrame);
        cat.Advance(TimeSpan.FromMilliseconds(499));
        Assert.Equal(2, cat.Pose.BangsFrame);
        cat.Advance(TimeSpan.FromMilliseconds(1));
        Assert.Equal(0, cat.Pose.BangsFrame);
        cat.Advance(TimeSpan.FromMilliseconds(4999));
        Assert.Equal(0, cat.Pose.BangsFrame);
        cat.Advance(TimeSpan.FromMilliseconds(1));
        Assert.Equal(0, cat.Pose.BangsFrame);
        cat.Advance(TimeSpan.FromMilliseconds(500));
        Assert.Equal(1, cat.Pose.BangsFrame);
    }

    [Fact]
    public void Quiet_speech_never_opens_the_mouth_fully()
    {
        var cat = Create(tuning => tuning.MouthFrameDuration = TimeSpan.FromMilliseconds(100));
        cat.SetLevel(0.08f);

        var seen = new HashSet<MouthFrame> { cat.Pose.Mouth };
        for (var i = 0; i < 24; i++)
        {
            cat.Advance(TimeSpan.FromMilliseconds(100));
            seen.Add(cat.Pose.Mouth);
        }

        Assert.Contains(MouthFrame.Closed, seen);
        Assert.Contains(MouthFrame.Half, seen);
        Assert.DoesNotContain(MouthFrame.Open, seen);
    }

    [Fact]
    public void Loud_speech_cycles_all_three_mouth_frames()
    {
        var cat = Create(tuning => tuning.MouthFrameDuration = TimeSpan.FromMilliseconds(100));
        cat.SetLevel(0.4f);

        var seen = new HashSet<MouthFrame> { cat.Pose.Mouth };
        for (var i = 0; i < 8; i++)
        {
            cat.Advance(TimeSpan.FromMilliseconds(100));
            seen.Add(cat.Pose.Mouth);
        }

        Assert.Equal(new[] { MouthFrame.Closed, MouthFrame.Half, MouthFrame.Open }, seen.OrderBy(frame => frame).ToArray());
    }

    [Fact]
    public void Dropping_to_quiet_speech_closes_a_fully_open_mouth()
    {
        var cat = Create(tuning => tuning.MouthFrameDuration = TimeSpan.FromMilliseconds(100));
        cat.SetLevel(0.4f);
        cat.Advance(TimeSpan.FromMilliseconds(200));
        Assert.Equal(MouthFrame.Open, cat.Pose.Mouth);

        cat.SetLevel(0.05f);
        Assert.Equal(MouthFrame.Closed, cat.Pose.Mouth);

        for (var i = 0; i < 10; i++)
        {
            cat.Advance(TimeSpan.FromMilliseconds(100));
            Assert.NotEqual(MouthFrame.Open, cat.Pose.Mouth);
        }
    }

    [Fact]
    public void Silence_keeps_the_mouth_closed()
    {
        var cat = Create();
        cat.SetLevel(0);
        cat.Advance(TimeSpan.FromSeconds(1));
        Assert.Equal(MouthFrame.Closed, cat.Pose.Mouth);
    }

    [Fact]
    public void Key_press_lifts_then_lowers_the_hand()
    {
        var cat = Create();
        Assert.Equal(HandPose.Idle, cat.Pose.Hand);

        cat.KeyDown("A");
        Assert.Equal(HandPose.Press, cat.Pose.Hand);
        Assert.Equal("A", cat.Pose.ActiveKey);

        cat.Advance(TimeSpan.FromMilliseconds(50));
        cat.KeyUp("A");
        cat.Advance(TimeSpan.FromMilliseconds(249));
        Assert.Equal(HandPose.Press, cat.Pose.Hand);

        cat.Advance(TimeSpan.FromMilliseconds(2));
        Assert.Equal(HandPose.Raised, cat.Pose.Hand);

        cat.Advance(TimeSpan.FromMilliseconds(698));
        Assert.Equal(HandPose.Raised, cat.Pose.Hand);

        cat.Advance(TimeSpan.FromMilliseconds(2));
        Assert.Equal(HandPose.Idle, cat.Pose.Hand);
    }

    [Fact]
    public void Holding_a_key_keeps_the_hand_up_until_release()
    {
        var cat = Create();
        cat.KeyDown("Space");
        cat.Advance(TimeSpan.FromMilliseconds(1500));
        Assert.Equal(HandPose.Raised, cat.Pose.Hand);

        cat.KeyUp("Space");
        cat.Advance(TimeSpan.FromMilliseconds(699));
        Assert.Equal(HandPose.Raised, cat.Pose.Hand);
        cat.Advance(TimeSpan.FromMilliseconds(2));
        Assert.Equal(HandPose.Idle, cat.Pose.Hand);
    }

    [Fact]
    public void Next_key_retargets_the_press()
    {
        var cat = Create();
        cat.KeyDown("A");
        cat.Advance(TimeSpan.FromMilliseconds(100));
        var first = cat.Pose.KeyX;

        cat.KeyDown("L");
        Assert.Equal(HandPose.Press, cat.Pose.Hand);
        Assert.Equal("L", cat.Pose.ActiveKey);
        Assert.NotEqual(first, cat.Pose.KeyX);

        cat.Advance(TimeSpan.FromMilliseconds(299));
        Assert.Equal(HandPose.Press, cat.Pose.Hand);
        cat.Advance(TimeSpan.FromMilliseconds(2));
        Assert.Equal(HandPose.Raised, cat.Pose.Hand);
        Assert.Equal("L", cat.Pose.ActiveKey);
    }

    [Fact]
    public void Auto_repeat_does_not_restart_the_press()
    {
        var cat = Create();
        cat.KeyDown("D");
        cat.Advance(TimeSpan.FromMilliseconds(250));
        cat.KeyDown("D");
        cat.Advance(TimeSpan.FromMilliseconds(60));
        Assert.Equal(HandPose.Raised, cat.Pose.Hand);
    }

    [Fact]
    public void Unknown_keys_still_move_the_hand()
    {
        var cat = Create();
        cat.KeyDown("MediaPlayPause");
        Assert.Equal(HandPose.Press, cat.Pose.Hand);
        Assert.Equal("center", cat.Pose.Zone);
    }

    [Fact]
    public void Spurious_key_up_does_not_raise_an_idle_hand()
    {
        var cat = Create();
        cat.KeyDown("A");
        cat.KeyUp("A");
        cat.Advance(TimeSpan.FromSeconds(3));
        Assert.Equal(HandPose.Idle, cat.Pose.Hand);

        cat.KeyUp("A");
        Assert.Equal(HandPose.Idle, cat.Pose.Hand);
    }

    [Fact]
    public void Blink_uses_three_frames_and_a_different_next_gap()
    {
        var cat = Create(
            tuning =>
            {
                tuning.BlinkGapMin = TimeSpan.FromMilliseconds(1000);
                tuning.BlinkGapMax = TimeSpan.FromMilliseconds(5000);
                tuning.BlinkFrameDuration = TimeSpan.FromMilliseconds(70);
                tuning.DoubleBlinkChance = 0;
            },
            new ScriptRandom([1000, 4000], [0.99]));

        Assert.Equal(0, cat.Pose.EyeFrame);
        cat.Advance(TimeSpan.FromMilliseconds(999));
        Assert.Equal(0, cat.Pose.EyeFrame);

        cat.Advance(TimeSpan.FromMilliseconds(1));
        Assert.Equal(1, cat.Pose.EyeFrame);
        cat.Advance(TimeSpan.FromMilliseconds(70));
        Assert.Equal(2, cat.Pose.EyeFrame);
        cat.Advance(TimeSpan.FromMilliseconds(70));
        Assert.Equal(1, cat.Pose.EyeFrame);
        cat.Advance(TimeSpan.FromMilliseconds(70));
        Assert.Equal(0, cat.Pose.EyeFrame);

        cat.Advance(TimeSpan.FromMilliseconds(3999));
        Assert.Equal(0, cat.Pose.EyeFrame);
        cat.Advance(TimeSpan.FromMilliseconds(1));
        Assert.Equal(1, cat.Pose.EyeFrame);
    }

    [Fact]
    public void Mouse_buttons_follow_the_latest_press()
    {
        var cat = Create();
        Assert.Equal(PointerButton.None, cat.Pose.Mouse);

        cat.MouseDown(PointerButton.Left);
        Assert.Equal(PointerButton.Left, cat.Pose.Mouse);
        cat.MouseDown(PointerButton.Right);
        Assert.Equal(PointerButton.Right, cat.Pose.Mouse);
        cat.MouseUp(PointerButton.Right);
        Assert.Equal(PointerButton.Left, cat.Pose.Mouse);
        cat.MouseUp(PointerButton.Left);
        Assert.Equal(PointerButton.None, cat.Pose.Mouse);
    }

    [Fact]
    public void Raised_hand_sits_above_the_pressed_key()
    {
        var idle = new CatPose(0, MouthFrame.Closed, 0, PointerButton.None, HandPose.Idle, null, "center", 0.5, 0.5);
        var press = idle with { Hand = HandPose.Press, ActiveKey = "A", KeyX = 0.5, KeyY = 0.5 };
        var raised = press with { Hand = HandPose.Raised };

        var idlePoint = HandPlacementMath.Resolve(idle, 1000, 500, 0.2, 0.4, 0.5, 0.2, 0.1, 0.8, 0.1);
        var pressPoint = HandPlacementMath.Resolve(press, 1000, 500, 0.2, 0.4, 0.5, 0.2, 0.1, 0.8, 0.1);
        var raisedPoint = HandPlacementMath.Resolve(raised, 1000, 500, 0.2, 0.4, 0.5, 0.2, 0.1, 0.8, 0.1);

        Assert.Equal(100, idlePoint.X);
        Assert.Equal(400, idlePoint.Y);
        Assert.Equal(450, pressPoint.X);
        Assert.Equal(250, pressPoint.Y);
        Assert.Equal(pressPoint.X, raisedPoint.X);
        Assert.Equal(200, raisedPoint.Y);
    }

    private static CatAnimator Create(Action<AnimationTuning>? edit = null, IRandomSource? random = null)
    {
        var tuning = new AnimationTuning
        {
            BangsFrameDuration = TimeSpan.FromMilliseconds(500),
            BangsPause = TimeSpan.FromSeconds(5),
            KeyPressDuration = TimeSpan.FromMilliseconds(300),
            KeyboardIdleDelay = TimeSpan.FromMilliseconds(1000),
            BlinkGapMin = TimeSpan.FromHours(1),
            BlinkGapMax = TimeSpan.FromHours(1),
            DoubleBlinkChance = 0,
            QuietThreshold = 0.035f,
            LoudThreshold = 0.14f
        };
        edit?.Invoke(tuning);
        return new CatAnimator(tuning, random ?? new ScriptRandom([3_600_000], [0.99]));
    }

    private sealed class ScriptRandom : IRandomSource
    {
        private readonly Queue<int> _ints;
        private readonly Queue<double> _doubles;

        public ScriptRandom(int[] ints, double[] doubles)
        {
            _ints = new Queue<int>(ints);
            _doubles = new Queue<double>(doubles);
        }

        public int NextInt(int minInclusive, int maxExclusive)
        {
            var value = _ints.Count > 0 ? _ints.Dequeue() : minInclusive;
            if (value < minInclusive)
                return minInclusive;
            if (value >= maxExclusive)
                return maxExclusive - 1;
            return value;
        }

        public double NextDouble() => _doubles.Count > 0 ? _doubles.Dequeue() : 0.99;
    }
}
