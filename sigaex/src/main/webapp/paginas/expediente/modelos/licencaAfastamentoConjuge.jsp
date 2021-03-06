<%@ taglib tagdir="/WEB-INF/tags/mod" prefix="mod"%>
<%@ taglib uri="http://localhost/functiontag" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<!-- este modelo trata de
LICENÇA PARA AFASTAMENTO DO CONJUGUE
< ESTE DOCUMENTO SE DIVIDE EM 3 PAGINAS >
[OBS AO PROGRAMADOR: CRIAR DIGITO OU METODO P/ SALTAR PAGINA]  -->

<c:set var="esconderTexto" value="sim" scope="request" />
<c:set var="para" value="diretorForo" scope="request" />
<mod:modelo urlBase="/paginas/expediente/modelos/requerimento_rh.jsp">
	<mod:documento>
		<mod:valor var="texto_requerimento">
			<p style="TEXT-INDENT: 2cm" align="justify">
			${doc.subscritor.descricao}, ${doc.subscritor.cargo.nomeCargo}, ${doc.subscritor.padraoReferenciaInvertido}, lotado(a) no(a) ${doc.subscritor.lotacao.descricao}, vem requerer a Vossa Excelência que se digne encaminhar 
			o requerimento de <b>LICENÇA POR MOTIVO DE AFASTAMENTO DO CÔNJUGE</b>, em anexo, ao E. Tribunal Regional Federal da 2ª Região.
			</p>				
		</mod:valor>
<!-- [Aqui tem q pular página]  -->			
		<mod:valor var="texto_requerimento2">
		<!-- <br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br> -->
			<c:import url="/paginas/expediente/modelos/inc_tit_presidTrf2aRegiao.jsp" />				
			<p style="TEXT-INDENT: 2cm" align="justify">
			${doc.subscritor.descricao}, ${doc.subscritor.cargo.nomeCargo}, ${doc.subscritor.padraoReferenciaInvertido}, lotado(a) no(a) ${doc.subscritor.lotacao.descricao}, vem requerer a Vossa Excelência, nos termos do art. 84 de Lei nº 8.112/90
			regulamentado pela Resolução nº 5/2008 do Conselho da Justiça Federal, <b>LICENÇA POR MOTIVO DE AFASTAMENTO DO CÔNJUGE</b>.
			</p>
			<p style="TEXT-INDENT: 2cm" align="justify">
			Declaro estar ciente dos termos da norma citada, especialmente no que diz respeito ao seguinte:
			</p>
			<p style="TEXT-INDENT: 2cm" align="justify">
			- que  a concessão será por prazo indeterminado, enquanto perdurar o vínculo
			matrimonial ou a união estável, comprometenso-se a encaminhar ao órgão de origem,
			anualmente, declaração que ateste o deslocamento e mautenção da situação mencionada
			(art. 68, parágrafo 1º c/c art. 71, parágrafo 3º);
			</p>
			<p style="TEXT-INDENT: 2cm" align="justify">
			- que o período de licença sem remuneração não será contado para nenhum efeito,
			exeto para aposentadoria na hipótese do art. 72, suspendendo o estágio probatório,
			a aquisição da estabilidade e a concessão de progressão ou promoção funcional (art. 69
			e parágrafo único);
			</p>
			<p style="TEXT-INDENT: 2cm" align="justify">
			- que será assegurada ao servidor licenciado a manutenção da vinculação ao regime do
			Plano de Seguridade Social do Servidor Público, mediante o recolhimento mensal da
			respectiva contribuição, no mesmo percentual devido pelos servidores em atividade,
			incidente sobre a remuneração total do cargo a que faz jus no exercício de suas
			atribuições, computando-se, para esse efeito, inclusive, as vantagens pessoais (art.
			72).
			</p>
						
			<c:import url="/paginas/expediente/modelos/inc_deferimento.jsp" />
			<p align="center">${doc.dtExtenso}</p> 			
			<c:import
			url="/paginas/expediente/modelos/inc_assinatura.jsp?formatarOrgao=sim" />
			
			<c:import url="/paginas/expediente/modelos/inc_classificacaoDocumental.jsp" />
			
		</mod:valor>
		
</mod:documento>
</mod:modelo>
