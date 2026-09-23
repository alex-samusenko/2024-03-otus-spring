using NAudio.Wave;

namespace BongoCat.App;

public sealed class MicrophoneMonitor : IDisposable
{
    private readonly object _gate = new();
    private WaveInEvent? _waveIn;
    private float _level;

    public float Level
    {
        get
        {
            lock (_gate)
                return _level;
        }
    }

    public IReadOnlyList<MicDevice> ListDevices()
    {
        var devices = new List<MicDevice>();
        try
        {
            for (var i = 0; i < WaveIn.DeviceCount; i++)
            {
                var caps = WaveIn.GetCapabilities(i);
                devices.Add(new MicDevice(i, caps.ProductName));
            }
        }
        catch
        {
            devices.Clear();
        }

        return devices;
    }

    public void Start(int deviceIndex)
    {
        Stop();
        if (WaveIn.DeviceCount <= 0)
            return;

        if (deviceIndex < 0 || deviceIndex >= WaveIn.DeviceCount)
            deviceIndex = 0;

        var wave = new WaveInEvent
        {
            DeviceNumber = deviceIndex,
            WaveFormat = new WaveFormat(16000, 16, 1),
            BufferMilliseconds = 50
        };
        wave.DataAvailable += OnData;
        wave.StartRecording();
        _waveIn = wave;
    }

    public void Stop()
    {
        if (_waveIn is null)
            return;

        try
        {
            _waveIn.DataAvailable -= OnData;
            _waveIn.StopRecording();
        }
        catch
        {
            // The device may already be gone.
        }

        _waveIn.Dispose();
        _waveIn = null;
        lock (_gate)
            _level = 0;
    }

    public void Dispose() => Stop();

    private void OnData(object? sender, WaveInEventArgs e)
    {
        if (e.BytesRecorded < 2)
            return;

        double sum = 0;
        var samples = e.BytesRecorded / 2;
        for (var i = 0; i < e.BytesRecorded - 1; i += 2)
        {
            var sample = BitConverter.ToInt16(e.Buffer, i) / 32768.0;
            sum += sample * sample;
        }

        var rms = (float)Math.Sqrt(sum / Math.Max(1, samples));
        lock (_gate)
            _level = _level * 0.62f + rms * 0.38f;
    }
}

public sealed record MicDevice(int Index, string Name)
{
    public override string ToString() => Name;
}
