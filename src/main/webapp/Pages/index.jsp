<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"%>
<!doctype html>
<html>
<head>
	<meta charset="UTF-8">
	<title>OpenBaas</title>
	<link rel='stylesheet' id='bootstrap-css'  href='http://maxcdn.bootstrapcdn.com/bootstrap/3.2.0/css/bootstrap.min.css?ver=3.2.0' type='text/css' media='all' />
	<link rel='stylesheet' id='style-css'  href='${pageContext.request.contextPath}/resources/style.css' type='text/css' media='all' />
	<link href='http://fonts.googleapis.com/css?family=Ubuntu:400,700' rel='stylesheet' type='text/css'>
</head>
<body>
	<header>
		<div class="container">
		<div class="row">
		<div class="col-xs-6 col-sm-6 	col-md-6">
		<a href="/" class="logo"><img src="${pageContext.request.contextPath}/resources/img/logo.png" alt="OpenBaas"  height="59"/></a>
		</div>
		<div class="col-xs-6 col-sm-6 	col-md-6 text-right">
		<a href="http://www.openbaas.com/" class="link-home">Back to OpenBaas Website ></a>
		</div>
		</div>
		</div>
	</header>
	<section class="banner">
		<div class="container">
			<div class="row">
				<div class="col-xs-12 col-sm-6 	col-md-6">
					<h1>OpenBaas on Openshift</h1>
					<br><br>
				  	<h4>To access the OpenBaas Platform use the Base URL: <br>
				  	"http://openbaas-infosistema.rhcloud.com/OpenBaas/"<br>
				  	<br> Use this access to make your first requests to Openbaas platform.
				  	<br><br> To learn more about how to make your web/mobile applications work with OpenBaas, go to the OpenBaas' Wiki</h4>
				  <div class="row">
				    <div class="col-xs-12 col-sm-12 	col-md-12">
				    	<a href="http://wiki.openbaas.com/"><img src="${pageContext.request.contextPath}/resources/img/wiki-button.png" alt="Wiki OpenBaas" height="75" class="img-responsive"/></a>
				    </div>
				  </div>
				</div>
				<div class="col-xs-12 col-sm-6 	col-md-6"><img src="${pageContext.request.contextPath}/resources/img/campaign-new.png" height="499" alt="app" class="img-responsive"/>
				</div>
			</div>
		</div>
	</section>
	<footer>
		<div class="container">
			<div class="row">
				<div class="col-xs-12 col-sm-12 	col-md-12">
					<p class="copyright">copyright 2014. <a href="http://www.openbaas.com/" target="_blank">OpenBaas</a>. Todos os direitos reservados.</p>
				</div>
			</div>
		</div>
	</footer>
	<script type='text/javascript' src='https://code.jquery.com/jquery-2.1.1.min.js?ver=2.1.1'></script>
	<script type='text/javascript' src='http://maxcdn.bootstrapcdn.com/bootstrap/3.2.0/js/bootstrap.min.js?ver=3.2.0'></script>
</body>
</html>