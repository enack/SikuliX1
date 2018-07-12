package org.sikuli.script;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Range;
import org.opencv.core.Scalar;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FindResult2 implements Iterator<Match> {

  private FindInput2 findInput = null;
  private int offX = 0;
  private int offY = 0;
  private Mat result = null;

  private FindResult2() {
  }

  public FindResult2(Mat result, FindInput2 findInput) {
    this.result = result;
    this.findInput = findInput;
  }

  public FindResult2(Mat result, FindInput2 target, int[] off) {
    this(result, target);
    offX = off[0];
    offY = off[1];
  }

  private Core.MinMaxLocResult resultMinMax = null;

  private double currentScore = -1;
  double firstScore = -1;
  double scoreMaxDiff = 0.001;

  private int currentX = -1;
  private int currentY = -1;

  public boolean hasNext() {
    resultMinMax = Core.minMaxLoc(result);
    currentScore = resultMinMax.maxVal;
    currentX = (int) resultMinMax.maxLoc.x;
    currentY = (int) resultMinMax.maxLoc.y;
    if (firstScore < 0) {
      firstScore = currentScore;
    }
    double targetScore = findInput.getScore();
    double scoreMin = firstScore - scoreMaxDiff;
    if (currentScore > targetScore && currentScore > scoreMin) {
      return true;
    }
    return false;
  }

  public Match next() {
    Match match = null;
    if (hasNext()) {
      match = new Match(currentX + offX, currentY + offY,
              findInput.getTarget().width(), findInput.getTarget().height(), currentScore, null);
      int margin = getPurgeMargin();
      Range rangeX = new Range(Math.max(currentX - margin, 0), currentX + 1);
      Range rangeY = new Range(Math.max(currentY - margin, 0), currentY + 1);
      result.colRange(rangeX).rowRange(rangeY).setTo(new Scalar(0f));
    }
    return match;
  }

  private int getPurgeMargin() {
    if (currentScore < 0.95) {
      return 4;
    } else if (currentScore < 0.85) {
      return 8;
    } else if (currentScore < 0.71) {
      return 16;
    }
    return 2;
  }

  double bestScore = 0;
  double meanScore = 0;
  double stdDevScore = 0;

  public List<Match> getMatches() {
    if (hasNext()) {
      List<Match> matches = new ArrayList<Match>();
      List<Double> scores = new ArrayList<>();
      while (true) {
        Match match = next();
        if (Do.SX.isNull(match)) {
          break;
        }
        meanScore = (meanScore * matches.size() + match.getScore()) / (matches.size() + 1);
        bestScore = Math.max(bestScore, match.getScore());
        matches.add(match);
        scores.add(match.getScore());
      }
      stdDevScore = calcStdDev(scores, meanScore);
      return matches;
    }
    return null;
  }

  public double[] getScores() {
    return new double[]{bestScore, meanScore, stdDevScore};
  }

  private double calcStdDev(List<Double> doubles, double mean) {
    double stdDev = 0;
    for (double doubleVal : doubles) {
      stdDev += (doubleVal - mean) * (doubleVal - mean);
    }
    return Math.sqrt(stdDev / doubles.size());
  }

  @Override
  public void remove() {
  }
}
