#!/usr/local/bin/Rscript
#library(tseries)
#library(MASS)
library(fitdistrplus)
library(hash)


ms <- c(
  "10s-high-5",
  "10s-low-5",
  "1s-high-5",
  "1s-low-5",
  "500ms-high-5",
  "500ms-low-5",
  "10s-high-1",
  "10s-low-1",
  "1s-high-1",
  "1s-low-1",
  "500ms-high-1",
  "500ms-low-1"
)

gamma_model <- hash()
normal_model <- hash()

# Load simple model JNI
load_model <- function() {
  mins <- c()
  maxs <- c()
  for (i in 1:length(ms)) {
    data=scan(paste("/home/acanino/Projects/model/points/drive/", ms[i], ".txt", sep=""))
    mins <- append(mins, (min(data)))
    maxs <- append(maxs, (max(data)))
    gamma_fit  <- fitdist(data, "gamma")
    gamma_model[ms[i]] <- gamma_fit
    normal_fit  <- fitdist(data, "norm")
    normal_model[ms[i]] <- normal_fit
  }
  gamma_model["min"] <- min(mins)
  gamma_model["max"] <- max(maxs)
  normal_model["min"] <- min(mins)
  normal_model["max"] <- max(maxs)
}

lookup_model <- function(name) {
  return(gamma_model[[name]])
}

sample_gamma_model <- function(name) {
  point <- rgamma(1, gamma_model[[name]]$estimate['shape'], rate=gamma_model[[name]]$estimate['rate'])
  return(point)
}

sample_normal_model <- function(name) {
  point <- rnorm(1, normal_model[[name]]$estimate['mean'], sd=normal_model[[name]]$estimate['sd'])
  return(point)
}


# Testing some simple distributions
test_dist <- function() {
  for (i in 1:length(ms)) {
    data=scan(paste("/home/acanino/Projects/model/points/drive/", ms[i], ".txt", sep=""))
    pdf(paste(ms[i], ".pdf", sep=""))
    plotdist(data, histo=TRUE, demp = TRUE)

    fit_w  <- fitdist(data, "weibull")
    fit_g  <- fitdist(data, "gamma")
    fit_ln <- fitdist(data, "lnorm")
    fit_n <- fitdist(data, "norm")

    print(fit_n)

    legend=c("weibull","gamma","lnorm", "norm")

    denscomp(list(fit_w, fit_g, fit_ln, fit_n), legendtext = legend)
    cdfcomp (list(fit_w, fit_g, fit_ln, fit_n), legendtext = legend)
    qqcomp  (list(fit_w, fit_g, fit_ln, fit_n), legendtext = legend)
    ppcomp  (list(fit_w, fit_g, fit_ln, fit_n), legendtext = legend)

    gamma_samples = c()
    normal_samples = c()

    print(ms[i])
    for (j in 1:1) {
      #gamma_points <- rgamma(10, fit_g$estimate['shape'], rate=fit_g$estimate['rate'])
      normal_points <- rnorm(1000, fit_n$estimate['mean'], sd=fit_n$estimate['sd'])
      #print(gamma_points)
      print(min(normal_points))
      #gamma_joules <- sum(gamma_points)
      #normal_joules <- sum(normal_points)
      #gamma_samples <- append(gamma_samples, gamma_joules)
      #normal_samples <- append(normal_samples, normal_joules)
    }
    #print(samples)
    #print(mean(gamma_samples))
    #print(mean(normal_samples))
  }
}

load_model()
#test_dist()

# Playing around with ARIMA
#series=ts(data)
#cleaned=tsclean(series)
#cleaned_ma=ma(cleaned, order=2)

#count_ma=ts(na.omit(cleaned_ma), frequency=10)
#decomp=stl(count_ma, s.window="periodic")
#deseasonal_cnt <- seasadj(decomp)

#adf.test(count_ma, alternative="stationary")

#count_d1=diff(deseasonal_cnt, differences=1)

#plot(count_d1)
#adf.test(count_d1, alternative="stationary")

#Acf(count_d1, main='ACF for Differenced Series')
#Pacf(count_d1, main='PACF for Differenced Series')

#fit<-auto.arima(deseasonal_cnt, seasonal=FALSE)

#tsdisplay(residuals(fit), lag.max=45, main='(3,1,1) Model Residuals')

#fcast <- forecast(fit, h=100)

#plot(fcast)

