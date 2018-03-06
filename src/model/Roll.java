package model;

public class Roll {

  public int      _windowLength;
  public double[] _window;

  public Roll(int windowLength) {
    _windowLength = windowLength;
    _window = new double[_windowLength];
    for (int i = 0; i < _windowLength; i++) {
      _window[i] = 0;
    }
  }

  public void update(double val) {
    for (int i = 0; i < _windowLength-1; i++) {
      _window[i] = _window[i+1];
    }
    _window[_windowLength-1] = val;
  }

  public double relativeDeviation() {
    double mean = 0.0;
    for (int i = 0; i < _windowLength; i++) {
      mean += _window[i];
    }
    mean /= _windowLength;
    return mean;
    /*
    double variance = 0.0;
    for (int i = 0; i < _windowLength; i++) {
      variance += Math.pow(_window[i] - mean, 2);
    }
    variance /= _windowLength;
    double deviation = Math.sqrt(variance);
    double relative = (deviation / mean) * 100.0;
    return relative;
    */
  }

}
