package de.innfactory.akkaliftml.train

import io.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel("TrainingModel")
case class TrainingModel(
                          @(ApiModelProperty@field)(position = 1) ratings: String = null,
                          @(ApiModelProperty@field)(position = 2, example="true") header: Boolean = true,
                          @(ApiModelProperty@field)(position = 3, example = "0.6") training: Double = 0.6,
                          @(ApiModelProperty@field)(position = 4, example = "0.2") validation: Double = 0.2,
                          @(ApiModelProperty@field)(position = 5, example = "0.2") test: Double = 0.2,
                          @(ApiModelProperty@field)(position = 6, value = "e.g [8, 16, 200]") ranks: Array[Int] = Array({200}),
                          @(ApiModelProperty@field)(position = 7, value = "e.g [0.2, 0.1]") lambdas: Array[Double] = Array({0.1}),
                          @(ApiModelProperty@field)(position = 8, value = "e.g [5,10,20,25]") iterations: Array[Int] = Array({10}),
                          @(ApiModelProperty@field)(position = 9, example = "MatrixFactorizationModel") model : String = "MatrixFactorizationModel",
                          @(ApiModelProperty@field)(position = 10, example = "local[*]") sparkMaster: String = "local[*]"
                        )
