package controllers

import play.api.mvc._
import helper.ElasticSearchHelper
import com.google.gson.Gson

object ApplicationController extends Controller {

  def index = Action {
    implicit request =>
      Ok(views.html.index())
  }

  def search(query: String) = Action {
    implicit request =>
      Ok(views.html.index(ElasticSearchHelper.search(query), query))
  }

  def searchJson(query: String) = Action {
    implicit request =>
      Ok(new Gson().toJson(ElasticSearchHelper.search(query))).as("application/json;charset=UTF-8")
  }

}