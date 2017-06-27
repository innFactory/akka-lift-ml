package de.innfactory.akkaliftml.train

import io.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel("TrainingModel")
case class TrainingModel(
                          @(ApiModelProperty@field)(position = 1) ratings: String,
                          @(ApiModelProperty@field)(position = 2, example="true") header: Boolean,
                          @(ApiModelProperty@field)(position = 3, example = "0.6") training: Double,
                          @(ApiModelProperty@field)(position = 4, example = "0.2") validation: Double,
                          @(ApiModelProperty@field)(position = 5, example = "0.2") test: Double,
                          @(ApiModelProperty@field)(position = 6, value = "e.g [8, 16, 200]") ranks: Array[Double],
                          @(ApiModelProperty@field)(position = 7, value = "e.g [0.2, 0.1]") lambdas: Array[Double],
                          @(ApiModelProperty@field)(position = 8, value = "e.g [5,10,20,25]") iterations: Array[Double],
                          @(ApiModelProperty@field)(position = 9, example = "MatrixFactorizationModel") model : String
                        )