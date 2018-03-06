package model;

import org.rosuda.JRI.*;

import org.apache.commons.cli.*;

import com.stoke.*;
import com.stoke.types.*;
import com.stoke.eval.*;

import java.io.*;
import java.util.*;

import java.util.concurrent.*;

public class Simulator {

  public static class SimulatedReward extends Reward {
    Simulator _sim;
    AeneasMachine _bandit;
    boolean _useOracle;
    boolean _usePessimist;
    double _errorRate;

    String _bestConfigName;
    String _worstConfigName;

    double _sla;

    boolean _flip = false;
    int _count = 0;

    public SimulatedReward(Simulator sim, boolean useOracle, boolean usePessimist, double errorRate, String bestConfigName, String worstConfigName, Constraint constraint) {
      super(null);
      _sim = sim;
      _useOracle = useOracle;
      _usePessimist = usePessimist;
      _errorRate = errorRate;
      _bestConfigName = bestConfigName;
      _worstConfigName = worstConfigName;

      if (constraint == null) {
        _sla = 0.0;
      } else {
        _sla = KnobValT.needDouble(constraint.getRequired());
      }
    }

    public void setBandit(AeneasMachine bandit) {
      _bandit = bandit;
    }

    @Override
    public double valuate() {
      double e = this.perInteractionEnergy();
      if (Double.compare(_sla, 0.0) == 0) {
        return e;
      } else {
        return this.batteryRate(e);
      }
    }

    String _lastConfig;

    @Override
    public double perInteractionEnergy() {
      _count++;

      int interval = KnobValT.needInteger(_bandit.internalRead("gps-interval"));
      if (interval != 10000 && interval != 1000 && interval != 500) {
        if (interval > 3000) {
          interval = 10000;
        } else if (interval <= 3000 && interval >= 500) {
          interval = 1000;
        } else {
          interval = 500;
        }
      }

      String configName = Simulator.configString(
        interval,
        KnobValT.needInteger(_bandit.internalRead("gps-priority")),
        KnobValT.needInteger(_bandit.internalRead("gps-batch")));

      _lastConfig = configName;

      //System.out.format("STOKE: Interaction:%d -- Sampling config: %s\n", _bandit.step(), configName);

      double joules = 0.0;
      if (Math.random() >= _errorRate) {
        if (_useOracle) {
          joules = _sim.drawSample(_bestConfigName);
        } else if (_usePessimist) {
          joules = _sim.drawSample(_worstConfigName);
        } else {
          joules = _sim.drawSample(configName);
        }
      } else {
        joules = _sim.randomSample();
      } 

      _cachedJoules = joules;

      return joules;
    }

    @Override 
    public double SLA() {
      return _sla;
    }
  }

  public static enum ErrorType {
    NORMAL,
    HEAVY;

    public static ErrorType toErrorType(String str) {
      switch (str) {
        case "NORMAL":
          return ErrorType.NORMAL;
        case "HEAVY":
          return ErrorType.HEAVY;
        default:
          throw new RuntimeException("Unknown ErrorType " + str);
      }
    }
  }

  public static final String R_MODEL = "/home/acanino/Projects/model/src/model/R/model.R";
  public static int SMOOTHING = 100;
  
  public double _minSample = 0.0;
  public double _maxSample = 0.0;
  public Random _rand = new Random();

  public Rengine _engine;

  public void loadModel() {
    _engine = new Rengine (new String [] {"--vanilla"}, false, null);
    if (!_engine.waitForR()) { 
      throw new RuntimeException("R engine failed to load!");
    }

    String exp = String.format("source('%s')", R_MODEL);
    REXP exp_load = _engine.eval(exp);
    if (exp_load == null) {
      throw new RuntimeException("R model failed to load!");
    }

    REXP e1 = _engine.eval("lookup_model('min')");
    REXP e2 = _engine.eval("lookup_model('max')");
    _minSample = e1.asDouble();
    _maxSample = e2.asDouble(); 
  }

  public void closeModel() {
    _engine.end();
    _engine = null;
  }

  public double drawSample(String configName) {
    String exp = String.format("sample_gamma_model('%s')", configName);
    REXP e = _engine.eval(exp);
    double reading = e.asDouble();
    if (Double.compare(reading, 0.0) < 0) {
      reading = 0.0;
    }
    return reading;
  }

  public double randomSample() {
    return _rand.nextInt((int)Math.round(_maxSample - _minSample)) + _minSample;
    /*
    switch (errorType) {
      case NORMAL:
        return _rand.nextInt((int)Math.round(_maxSample - _minSample)) + _minSample;
      case HEAVY:
        double minRange = (_maxSample * 0.5);
        double maxRange = (_maxSample * 1.5);
        return _rand.nextInt((int)Math.round(maxRange - minRange)) + minRange;
      default:
        throw new RuntimeException("Uh Oh!");
    }
    */
  }

  public static String configString(int update, int priority, int batch) {
    String priorityStr = "";
    switch (priority) {
      case 0:
        priorityStr = "low";
        break;
      case 1:
        priorityStr = "balanced";
        break;
      case 2:
        priorityStr = "high";
        break;
    }

    String updateSuf = "";
    if (update < 1000) {
      updateSuf = "ms";
    } else {
      updateSuf = "s";
      update = update / 1000;
    }

    StringBuilder sb = new StringBuilder();
    sb.append(update);
    sb.append(updateSuf + "-");
    sb.append(priorityStr + "-");
    sb.append(batch);

    return sb.toString();
  }

  public static String arraytoString(Object[] arr) {
    String str = "";
    for (int i = 0; i < arr.length; i++) {
      str += String.format("%s ", arr[i]);
    }
    return str;
  }

  public static void main(String[] args) {
    Options options = new Options();
    options.addOption(
        OptionBuilder.withArgName("num")
                     .hasArg()
                     .withDescription("number of bandit runs")
                     .create("runs"));
    options.addOption(
        OptionBuilder.withArgName("num")
                     .hasArg()
                     .withDescription("number of bandit interactions per run")
                     .create("interactions"));
    options.addOption(
        OptionBuilder.withArgName("policy")
                     .hasArg()
                     .withDescription("stochastic policy to use (NO_STOCHASTIC, EPSILON_GREEDY, VDBE)")
                     .create("stochastic"));
    options.addOption(
        OptionBuilder.withArgName("num")
                     .hasArg()
                     .withDescription("number of samples drawn during initial configuration phase")
                     .create("samples"));

    options.addOption(
        OptionBuilder.withArgName("prio")
                     .hasArg()
                     .withDescription("low, high, or all")
                     .create("gpsPriority"));

    options.addOption(
        OptionBuilder.withArgName("batch")
                     .hasArg()
                     .withDescription("5x, 1x, or all")
                     .create("gpsBatch"));

    options.addOption(
        OptionBuilder.withArgName("interval")
                     .hasArg()
                     .withDescription("wide, narrow, or all")
                     .create("gpsInterval"));

    options.addOption(
        OptionBuilder.withArgName("rate")
                     .hasArg()
                     .withDescription("rate between 0 and 1 for errors")
                     .create("errorRate"));

    options.addOption(
        OptionBuilder.withArgName("type")
                     .hasArg()
                     .withDescription("types of errors (NORMAL, HEAVY)")
                     .create("errorType"));

    options.addOption(
        OptionBuilder.withArgName("value")
                     .hasArg()
                     .withDescription("constraint for bandit")
                     .create("constraint"));

    options.addOption(
        OptionBuilder.withArgName("value")
                     .hasArg()
                     .withDescription("constraint for bandit")
                     .create("constraint2"));

    options.addOption(
        OptionBuilder.withArgName("name")
                     .hasArg()
                     .withDescription("1arm_wide, 1arm_narrow, 2arm, 3arm")
                     .create("profile"));

    options.addOption(
        OptionBuilder.withArgName("rev")
                     .hasArg()
                     .withDescription("true or false")
                     .create("rev"));

    options.addOption(
        OptionBuilder.withArgName("auto")
                     .hasArg()
                     .withDescription("true or false")
                     .create("useAuto"));

    options.addOption(
        OptionBuilder.withArgName("inferred")
                     .hasArg()
                     .withDescription("true or false")
                     .create("useInferred"));


    options.addOption(new Option("help", "print help message"));

    int numRuns = 0;
    int numInteractions = 0;
    int numSamples = 2;
    double errorRate = 0.0;
    boolean useOracle = false;
    boolean usePessimist = false;
    boolean useAuto = false;
    boolean useInferred = false;
    ErrorType errorType = ErrorType.NORMAL;

    StochasticPolicyType stochasticPolicyType = StochasticPolicyType.NO_STOCHASTIC;
    SamplingPolicy samplingPolicy = SamplingPolicy.SAMPLE_ALL;

    Integer[] prioritySettings = null;
    Integer[] intervalSettings = null;
    Integer[] batchSettings = null;

    Constraint constraint = null;

    CommandLineParser parser = new DefaultParser(); 
    try {
      CommandLine line = parser.parse(options, args);

      if (line.hasOption("help")) {
        new HelpFormatter().printHelp("sim", options);
        System.exit(1);
      }
        
      numRuns = Integer.parseInt(line.getOptionValue("runs"));
      numInteractions = Integer.parseInt(line.getOptionValue("interactions"));

      if (line.hasOption("samples")) {
        numSamples = Integer.parseInt(line.getOptionValue("samples"));
      }

      if (line.hasOption("errorRate")) {
        errorRate = Double.parseDouble(line.getOptionValue("errorRate"));
      }

      if (line.hasOption("errorType")) {
        errorType = ErrorType.toErrorType(line.getOptionValue("errorType"));
      }

      if (line.hasOption("stochastic")) {
        String policyType = line.getOptionValue("stochastic");
        if (policyType.equals("ORACLE")) {
          useOracle = true;
          stochasticPolicyType = StochasticPolicyType.NO_STOCHASTIC;
        } else if (policyType.equals("PESSIMIST")) {
          usePessimist = true;
          stochasticPolicyType = StochasticPolicyType.NO_STOCHASTIC;
        } else {
          stochasticPolicyType = StochasticPolicyType.toStochasticPolicy(line.getOptionValue("stochastic"));
        }
      }

      if (line.hasOption("profile")) {
        String profile = line.getOptionValue("profile");
        switch (profile) {
          case "1arm_wide":
            intervalSettings = new Integer[]{10000, 500};
            batchSettings = new Integer[]{1};
            prioritySettings = new Integer[]{2};
            break;
          case "1arm_narrow":
            intervalSettings = new Integer[]{1000, 500};
            batchSettings = new Integer[]{1};
            prioritySettings = new Integer[]{2};
            break;
          case "2arm":
            intervalSettings = new Integer[]{10000, 1000, 500};
            batchSettings = new Integer[]{1};
            prioritySettings = new Integer[]{0, 2};
            break;
          case "3arm":
            intervalSettings = new Integer[]{10000, 1000, 500};
            batchSettings = new Integer[]{5, 1};
            prioritySettings = new Integer[]{0, 2};
            break;
          case "test":
            intervalSettings = new Integer[]{10000, 1000, 500};
            batchSettings = new Integer[]{1};
            prioritySettings = new Integer[]{2};
            break;
        }
      }

      if (line.hasOption("gpsInterval")) {
        String gpsInterval = line.getOptionValue("gpsInterval");
        switch (gpsInterval) {
          case "wide":
            intervalSettings = new Integer[]{10000, 500};
            break;
          case "narrow":
            intervalSettings = new Integer[]{1000, 500};
            break;
          case "all":
            intervalSettings = new Integer[]{10000, 1000, 500};
            break;
        }
      }

      if (line.hasOption("gpsBatch")) {
        String gpsInterval = line.getOptionValue("gpsBatch");
        switch (gpsInterval) {
          case "5x":
            batchSettings = new Integer[]{5};
            break;
          case "1x":
            batchSettings = new Integer[]{1};
            break;
          case "all":
            batchSettings = new Integer[]{5, 1};
            break;
        }
      }

      if (line.hasOption("gpsPriority")) {
        String gpsInterval = line.getOptionValue("gpsPriority");
        switch (gpsInterval) {
          case "low":
            prioritySettings = new Integer[]{0};
            break;
          case "high":
            prioritySettings = new Integer[]{2};
            break;
          case "all":
            prioritySettings = new Integer[]{0, 2};
            break;
        }
      } 

      if (line.hasOption("useAuto")) {
        useAuto = true;
      }

      if (line.hasOption("useInferred")) {
        useInferred = true;
      }


      if (line.hasOption("constraint")) {
        double cons = Double.parseDouble(line.getOptionValue("constraint"));
        constraint = new Constraint(new Recording("joule", KnobValType.DOUBLE), KnobValT.haveDouble(cons), true);
      }

      if (line.hasOption("constraint2")) {
        double cons = Double.parseDouble(line.getOptionValue("constraint2"));
        constraint = new Constraint(new Recording("joule", KnobValType.DOUBLE), KnobValT.haveDouble(cons), true);
      }

      if (line.hasOption("rev")) {
        String rev = line.getOptionValue("rev");
        if (rev.equals("true")) {
          samplingPolicy = SamplingPolicy.SAMPLE_REVERSE;
        }
      }


    } catch (Exception e) {
      System.out.println("ERROR: Parsing failed: " + e.getMessage());
      System.exit(1);
    } 

    if (prioritySettings == null || intervalSettings == null || batchSettings == null) {
      System.out.println("ERROR: All settings must be defined (through individual parameters or an application profile");
      System.exit(1);
    }

    Knob priorityKnob = new DiscreteKnob("gps-priority", KnobValT.haveIntegers(prioritySettings));

    Knob updateKnob = null;
    if (useInferred) {
      updateKnob = new InferredKnob("gps-interval", 10000, 500, new Integer[]{1000});
    } else {
      updateKnob = new DiscreteKnob("gps-interval", KnobValT.haveIntegers(intervalSettings));
    }
    
    Knob batchKnob = new DiscreteKnob("gps-batch", KnobValT.haveIntegers(batchSettings));

    String bestConfigName = Simulator.configString(
        intervalSettings[0],
        prioritySettings[0],
        batchSettings[0]
        ); 

      String worstConfigName = Simulator.configString(
        intervalSettings[intervalSettings.length-1],
        prioritySettings[prioritySettings.length-1],
        batchSettings[batchSettings.length-1]
        ); 


    int interactionsPerRun = numInteractions + 
      (numSamples * batchSettings.length * prioritySettings.length * intervalSettings.length) + 1;

    System.out.format("Simulation Parameters...\n");
    System.out.format("  Runs: %d\n", numRuns);
    System.out.format("  Interactions: %d\n", numInteractions);
    System.out.format("  Samples: %d\n", numSamples);
    System.out.format("  Error Rate: %.2f\n", errorRate);
    System.out.format("  Error Type: %s\n", errorType.toString());
    System.out.format("  Stochastic Policy: %s\n", stochasticPolicyType);
    System.out.format("  Sampling Policy: %s\n", samplingPolicy);
    System.out.format("  GPS Interval Setting: %s\n", Simulator.arraytoString(intervalSettings));
    System.out.format("  GPS Priority Setting: %s\n", Simulator.arraytoString(prioritySettings));
    System.out.format("  GPS Batch Setting: %s\n", Simulator.arraytoString(batchSettings));

    Simulator sim = new Simulator();
    sim.loadModel();

    SimulatedReward simReward = new SimulatedReward(sim, useOracle, usePessimist, errorRate, bestConfigName, worstConfigName, constraint);

    AeneasMachine bandit = new AeneasMachine(
      new Knob[]{updateKnob, priorityKnob, batchKnob},
      new Recording[]{},
      constraint,
      stochasticPolicyType,
      samplingPolicy,
      simReward,
      Experiment.MACHINE,
      false);

    simReward.setBandit(bandit);

    bandit.setTaskDelay(0);
    //bandit.setTaskSkip(4);
    bandit.setNumTaskSamples(numSamples);
    bandit.setRepeatTaskReset(numInteractions);
    bandit.setContinuousSteps(10);
    bandit.setContinuousRounds(1);

    if (useAuto) {
      bandit.setRepeatTotal(numRuns);
    } else {
      bandit.setRepeatTotal(0);
    }

    System.out.format("Running: ");

    if (useAuto) {
      bandit.start();
    }

    if (useInferred) {
      interactionsPerRun += 100;
    }

    for (int i = 0; i < numRuns; i++) {
      System.out.format("I");

      int randomCount = 0;

      double bestReward = 0.0;

      boolean hit = false;
      boolean flip = false;

      if (!useAuto) {
        bandit.start();
      }

      for (int k = 0; k < interactionsPerRun; k++) {
        // Simply trigger an Aeneas update
        int interval = KnobValT.needInteger(bandit.read("gps-interval"));
        if (interval != 10000 && interval != 1000 && interval != 500) {
          if (interval > 3000) {
            interval = 10000;
          } else if (interval <= 3000 && interval >= 500) {
            interval = 1000;
          } else {
            interval = 500;
          }
        }

        int prio = KnobValT.needInteger(bandit.read("gps-priority"));
        int batch = KnobValT.needInteger(bandit.read("gps-batch"));

        bandit.interact();

        //if (k < 4) {
         // continue;
        //}

        String configName = Simulator.configString(interval, prio, batch);

        if (!simReward._lastConfig.equals(configName)) {
          System.err.format("Error on interaction %d on run %d\n", i, k);
          throw new RuntimeException(String.format("Error: Configuration mismatch: Sampled:%s Read:%s\n", simReward._lastConfig, configName));
        }

        bestReward += sim.drawSample(bestConfigName);

        double regret = bandit.totalReward() - bestReward;

        if (k == interactionsPerRun-1) {
          continue;
        }
        StringBuilder sb = new StringBuilder("Best:");
        sb.append(bestReward);
        sb.append(" Regret:");
        sb.append(regret);
        sb.append(" Relative:");
        sb.append(0.0);
        sb.append("\n");
        LogUtil.writeLogger(sb.toString());
      }

      if (!useAuto) {
        bandit.stop();
        bandit.reset();
      }
    }

    sim.closeModel();
    if (!useAuto) {
      bandit.done();
    }
  }

}
