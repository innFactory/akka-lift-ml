package de.innfactory.akkaliftml.als

import io.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel("AlsModel")
case class AlsModel(
                          @(ApiModelProperty@field)(position = 1) ratings: String = null,
                          @(ApiModelProperty@field)(position = 2, example = "default is 0.8") training: Option[Double] = None,
                          @(ApiModelProperty@field)(position = 3, example = "default is 0.1") validation:Option[ Double] = None,
                          @(ApiModelProperty@field)(position = 4, example = "default is 0.1") test: Option[Double] = None,
                          @(ApiModelProperty@field)(position = 5, value = "e.g [8, 16, 200]") ranks: Option[Array[Int]] = None,
                          @(ApiModelProperty@field)(position = 6, value = "e.g [0.2, 0.1]") lambdas: Option[Array[Double]] = None,
                          @(ApiModelProperty@field)(position = 7, value = "e.g [5,10,20,25]") iterations: Option[Array[Int]] = None,
                          @(ApiModelProperty@field)(position = 8, value = "e.g [0.1, 0.2]") alphas: Option[Array[Double]] = None,
                          @(ApiModelProperty@field)(position = 9, example = "local[*]") sparkMaster: Option[String] = None,
                          @(ApiModelProperty@field)(position = 10, example = "false") trainImplicit : Option[Boolean] =  None
                        )
