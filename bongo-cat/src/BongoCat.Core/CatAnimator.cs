namespace BongoCat.Core;

public sealed class CatAnimator
{
    private static readonly MouthFrame[] QuietCycle = [MouthFrame.Closed, MouthFrame.Half];
    private static readonly MouthFrame[] LoudCycle = [MouthFrame.Closed, MouthFrame.Half, MouthFrame.Open, MouthFrame.Half];
    private static readonly int[] BlinkSequence = [1, 2, 1];

    private readonly IRandomSource _random;
    private readonly List<string> _held = new();

    private TimeSpan _now;
    private TimeSpan _bangsTime;
    private TimeSpan _pressUntil;
    private TimeSpan _raisedUntil;
    private string? _activeKey;
    private bool _keyboardUsed;

    private bool _leftDown;
    private bool _rightDown;
    private PointerButton _recentMouse = PointerButton.None;

    private float _level;
    private MouthBand _band = MouthBand.Silent;
    private int _mouthStep;
    private TimeSpan _mouthElapsed;

    private EyeState _eyeState;
    private int _eyeFrame;
    private int _blinkStep;
    private bool _doublePending;
    private TimeSpan _eyeElapsed;
    private TimeSpan _eyeStateDuration;

    public CatAnimator(AnimationTuning tuning, IRandomSource? random = null)
    {
        Tuning = tuning;
        _random = random ?? new SystemRandomSource();
        ScheduleBlinkWait();
        Pose = Build();
    }

    public AnimationTuning Tuning { get; }

    public CatPose Pose { get; private set; }

    public TimeSpan Clock => _now;

    public void Advance(TimeSpan delta)
    {
        if (delta < TimeSpan.Zero)
            delta = TimeSpan.Zero;

        _now += delta;
        _bangsTime += delta;
        AdvanceEyes(delta);
        AdvanceMouth(delta);
        Pose = Build();
    }

    public void KeyDown(string key)
    {
        if (string.IsNullOrWhiteSpace(key) || _held.Contains(key))
            return;

        _held.Add(key);
        _activeKey = key;
        _keyboardUsed = true;
        _pressUntil = _now + Clamp(Tuning.KeyPressDuration, 20);
        var idle = Clamp(Tuning.KeyboardIdleDelay, 20);
        var press = Clamp(Tuning.KeyPressDuration, 20);
        if (idle < press + TimeSpan.FromMilliseconds(40))
            idle = press + TimeSpan.FromMilliseconds(40);
        _raisedUntil = _now + idle;
        Pose = Build();
    }

public void KeyUp(string key)
{
    if (!_held.Remove(key))
        return;

        if (_held.Count > 0)
        {
            if (key == _activeKey)
                _activeKey = _held[^1];
        }
        else if (_keyboardUsed)
        {
            var press = Clamp(Tuning.KeyPressDuration, 20);
            var idle = Clamp(Tuning.KeyboardIdleDelay, 20);
            var tail = idle - press;
            if (tail < TimeSpan.FromMilliseconds(40))
                tail = TimeSpan.FromMilliseconds(40);
            var until = _now + tail;
            if (until > _raisedUntil)
                _raisedUntil = until;
        }

        Pose = Build();
    }

    public void MouseDown(PointerButton button)
    {
        switch (button)
        {
            case PointerButton.Left:
                _leftDown = true;
                _recentMouse = PointerButton.Left;
                break;
            case PointerButton.Right:
                _rightDown = true;
                _recentMouse = PointerButton.Right;
                break;
        }

        Pose = Build();
    }

    public void MouseUp(PointerButton button)
    {
        switch (button)
        {
            case PointerButton.Left:
                _leftDown = false;
                if (_rightDown)
                    _recentMouse = PointerButton.Right;
                break;
            case PointerButton.Right:
                _rightDown = false;
                if (_leftDown)
                    _recentMouse = PointerButton.Left;
                break;
        }

        Pose = Build();
    }

    public void SetLevel(float level)
    {
        if (level < 0)
            level = 0;
        else if (level > 1)
            level = 1;

        _level = level;
        var band = Classify(level);
        if (band == _band)
            return;

        _band = band;
        _mouthStep = 0;
        _mouthElapsed = TimeSpan.Zero;
        Pose = Build();
    }

    private CatPose Build()
    {
        KeyboardLayout.TryGet(_activeKey, out var anchor);
        return new CatPose(
            BangsFrame(),
            CurrentMouth(),
            _eyeFrame,
            CurrentMouse(),
            CurrentHand(),
            _activeKey,
            anchor.Zone,
            anchor.X,
            anchor.Y);
    }

    private int BangsFrame()
    {
        var frame = Clamp(Tuning.BangsFrameDuration, 20);
        var pause = Tuning.BangsPause < TimeSpan.Zero ? TimeSpan.Zero : Tuning.BangsPause;
        var motion = TimeSpan.FromTicks(frame.Ticks * 3);
        var cycle = motion + pause;
        if (cycle <= TimeSpan.Zero)
            return 0;

        var t = _bangsTime.Ticks % cycle.Ticks;
        if (t < 0)
            t += cycle.Ticks;
        if (t >= motion.Ticks)
            return 0;

        var index = (int)(t / frame.Ticks);
        return index > 2 ? 2 : index;
    }

    private HandPose CurrentHand()
    {
        if (!_keyboardUsed || _activeKey is null)
            return HandPose.Idle;

        if (_now < _pressUntil)
            return HandPose.Press;

        if (_held.Count > 0 || _now < _raisedUntil)
            return HandPose.Raised;

        return HandPose.Idle;
    }

    private PointerButton CurrentMouse()
    {
        if (_leftDown && _rightDown)
            return _recentMouse;
        if (_leftDown)
            return PointerButton.Left;
        if (_rightDown)
            return PointerButton.Right;
        return PointerButton.None;
    }

    private MouthFrame CurrentMouth()
    {
        if (_band == MouthBand.Silent)
            return MouthFrame.Closed;

        var cycle = _band == MouthBand.Loud ? LoudCycle : QuietCycle;
        return cycle[_mouthStep % cycle.Length];
    }

    private void AdvanceMouth(TimeSpan delta)
    {
        if (_band == MouthBand.Silent)
        {
            _mouthStep = 0;
            _mouthElapsed = TimeSpan.Zero;
            return;
        }

        var duration = Clamp(Tuning.MouthFrameDuration, 20);
        var cycle = _band == MouthBand.Loud ? LoudCycle : QuietCycle;
        _mouthElapsed += delta;
        for (var i = 0; i < 40 && _mouthElapsed >= duration; i++)
        {
            _mouthElapsed -= duration;
            _mouthStep = (_mouthStep + 1) % cycle.Length;
        }
    }

    private MouthBand Classify(float level)
    {
        var quiet = Math.Clamp(Tuning.QuietThreshold, 0f, 1f);
        var loud = Math.Clamp(Tuning.LoudThreshold, quiet, 1f);
        var release = quiet * 0.65f;
        return _band switch
        {
            MouthBand.Silent when level >= loud && level >= quiet => MouthBand.Loud,
            MouthBand.Silent when level >= quiet => MouthBand.Quiet,
            MouthBand.Quiet when level >= loud => MouthBand.Loud,
            MouthBand.Quiet when level < release => MouthBand.Silent,
            MouthBand.Loud when level < release => MouthBand.Silent,
            MouthBand.Loud when level < loud * 0.8f => MouthBand.Quiet,
            _ => _band
        };
    }

    private void AdvanceEyes(TimeSpan delta)
    {
        var left = delta;
        for (var i = 0; i < 16 && left > TimeSpan.Zero; i++)
        {
            var remain = _eyeStateDuration - _eyeElapsed;
            if (remain > left)
            {
                _eyeElapsed += left;
                return;
            }

            left -= remain < TimeSpan.Zero ? TimeSpan.Zero : remain;
            _eyeElapsed = TimeSpan.Zero;
            switch (_eyeState)
            {
                case EyeState.Waiting:
                    _doublePending = _random.NextDouble() < Math.Clamp(Tuning.DoubleBlinkChance, 0, 1);
                    StartBlink();
                    break;
                case EyeState.Blinking:
                    _blinkStep++;
                    if (_blinkStep < BlinkSequence.Length)
                    {
                        _eyeFrame = BlinkSequence[_blinkStep];
                        _eyeStateDuration = BlinkFrameLength();
                    }
                    else if (_doublePending)
                    {
                        _doublePending = false;
                        _eyeState = EyeState.DoubleGap;
                        _eyeFrame = 0;
                        _eyeStateDuration = Clamp(Tuning.DoubleBlinkGap, 20);
                    }
                    else
                    {
                        ScheduleBlinkWait();
                    }

                    break;
                default:
                    StartBlink();
                    break;
            }
        }
    }

    private void ScheduleBlinkWait()
    {
        _eyeState = EyeState.Waiting;
        _eyeFrame = 0;
        _eyeElapsed = TimeSpan.Zero;
        _eyeStateDuration = RandomGap();
    }

    private void StartBlink()
    {
        _eyeState = EyeState.Blinking;
        _blinkStep = 0;
        _eyeFrame = BlinkSequence[0];
        _eyeElapsed = TimeSpan.Zero;
        _eyeStateDuration = BlinkFrameLength();
    }

    private TimeSpan RandomGap()
    {
        var min = (int)Math.Max(0, Tuning.BlinkGapMin.TotalMilliseconds);
        var max = (int)Math.Max(min, Tuning.BlinkGapMax.TotalMilliseconds);
        if (max == min)
            return TimeSpan.FromMilliseconds(min);

        return TimeSpan.FromMilliseconds(_random.NextInt(min, max + 1));
    }

    private TimeSpan BlinkFrameLength() => Clamp(Tuning.BlinkFrameDuration, 15);

    private static TimeSpan Clamp(TimeSpan value, int minMilliseconds)
    {
        var min = TimeSpan.FromMilliseconds(minMilliseconds);
        return value < min ? min : value;
    }

    private enum MouthBand
    {
        Silent,
        Quiet,
        Loud
    }

    private enum EyeState
    {
        Waiting,
        Blinking,
        DoubleGap
    }
}
