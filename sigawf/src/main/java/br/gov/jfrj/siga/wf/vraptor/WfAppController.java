package br.gov.jfrj.siga.wf.vraptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import com.crivano.jflow.model.enm.ProcessInstanceStatus;
import com.crivano.jflow.model.enm.VariableEditingKind;

import br.com.caelum.vraptor.Controller;
import br.com.caelum.vraptor.Get;
import br.com.caelum.vraptor.Path;
import br.com.caelum.vraptor.Post;
import br.com.caelum.vraptor.Result;
import br.gov.jfrj.siga.base.AplicacaoException;
import br.gov.jfrj.siga.base.SigaCalendar;
import br.gov.jfrj.siga.cp.model.DpLotacaoSelecao;
import br.gov.jfrj.siga.cp.model.DpPessoaSelecao;
import br.gov.jfrj.siga.dp.DpLotacao;
import br.gov.jfrj.siga.dp.DpPessoa;
import br.gov.jfrj.siga.vraptor.SigaIdDescr;
import br.gov.jfrj.siga.vraptor.SigaObjects;
import br.gov.jfrj.siga.vraptor.StringQualquer;
import br.gov.jfrj.siga.vraptor.Transacional;
import br.gov.jfrj.siga.wf.bl.Wf;
import br.gov.jfrj.siga.wf.bl.WfBL;
import br.gov.jfrj.siga.wf.dao.WfDao;
import br.gov.jfrj.siga.wf.model.WfDefinicaoDeDesvio;
import br.gov.jfrj.siga.wf.model.WfDefinicaoDeProcedimento;
import br.gov.jfrj.siga.wf.model.WfDefinicaoDeTarefa;
import br.gov.jfrj.siga.wf.model.WfDefinicaoDeVariavel;
import br.gov.jfrj.siga.wf.model.WfMov;
import br.gov.jfrj.siga.wf.model.WfMovAnotacao;
import br.gov.jfrj.siga.wf.model.WfProcedimento;
import br.gov.jfrj.siga.wf.model.enm.WfPrioridade;
import br.gov.jfrj.siga.wf.util.WfTarefa;
import br.gov.jfrj.siga.wf.util.WfTaskVO;
import br.gov.jfrj.siga.wf.util.WfUtil;

@Controller
@SuppressWarnings("unchecked")
public class WfAppController extends WfController {
	private static final String VERIFICADOR_ACESSO = "INI:Iniciar";

	/**
	 * @deprecated CDI eyes only
	 */
	public WfAppController() {
		super();
	}

	@Inject
	public WfAppController(HttpServletRequest request, Result result, WfDao dao, SigaObjects so, WfUtil util) {
		super(request, result, dao, so, util);
	}

	@Get
	@Path("/app/ativos")
	public void ativos() throws Exception {
		inbox();
	}

	public WfDao dao() {
		return (WfDao) dao;
	}

	/**
	 * Coloca as tarefas que estão para o usuário no atributo "taskInstances". Além
	 * disso, coloca os procedimentos que podem ser iniciados pelo usuário do
	 * sistema no atributo "processDefinitions".Este método é executando ao ser
	 * chamada a action "inbox.action".
	 * 
	 * @return
	 * @throws Exception
	 */
	@Get
	@Path("/app/inbox")
	public void inbox() throws Exception {
		SortedSet<WfTarefa> tis = new TreeSet<>();
		List<WfProcedimento> pis = dao().consultarProcedimentosPorPessoaOuLotacao(getTitular(), getLotaTitular());
		for (WfProcedimento pi : pis) {
			tis.add(new WfTarefa(pi));
		}
		result.include("taskInstances", tis);
	}

	/**
	 * Cria uma instância de processo baseando-se na definição de processo escolhida
	 * pelo usuário.
	 * 
	 * @return
	 * @throws Exception
	 */
	@Get
	@Path("/app/listar-para-iniciar")
	public void listarParaIniciar() throws Exception {
		assertAcesso(VERIFICADOR_ACESSO);
		List<WfDefinicaoDeProcedimento> modelos = dao().listarAtivos(WfDefinicaoDeProcedimento.class, "nome");
		result.include("itens", modelos);
	}

	/**
	 * Cria uma instância de processo baseando-se na definição de processo escolhida
	 * pelo usuário.
	 * 
	 * @return
	 * @throws Exception
	 */
	@Get
	@Path("/app/iniciar/{pdId}")
	public void iniciar(Long pdId) throws Exception {
		if (pdId == null)
			throw new RuntimeException();
		WfDefinicaoDeProcedimento pd = dao().consultar(pdId, WfDefinicaoDeProcedimento.class, false);
		result.include("pd", pd);
	}

	/**
	 * Cria uma instância de processo baseando-se na definição de processo escolhida
	 * pelo usuário.
	 * 
	 * @return
	 * @throws Exception
	 */
	@Transacional
	@Post
	@Path("/app/iniciar/{pdId}")
	public void iniciar(Long pdId, String principal) throws Exception {
		if (pdId == null)
			throw new RuntimeException();
		WfProcedimento pi = Wf.getInstance().getBL().createProcessInstance(pdId, getTitular(), getLotaTitular(),
				getIdentidadeCadastrante(), principal, null, null, true);
		result.redirectTo(this).procedimento(pi.getId());
	}

	/**
	 * Prove uma url de teste para a inbox do workflow.
	 * 
	 * @return
	 * @throws Exception
	 */
	@Path("/public/app/testes/gadgetTest")
	public void test(String matricula) throws Exception {
		DpPessoa pes = so.daoPes(matricula);
		so.setTitular(pes);
		so.setLotaTitular(pes.getLotacao());
		result.forwardTo(this).inbox();
	}

	/**
	 * Action que inicializa todos os campos que serão necessários para finalizar a
	 * tarefa especificada pelo Id do ProcessoInstance e o Id do ProcessDefinition.
	 * 
	 * @return Action.SUCCESS
	 */
	@Path("/app/procedimento/{piId}")
	public void procedimento(Long piId) throws Exception {
		WfProcedimento pi = loadTaskInstance(piId);
		List<SigaIdDescr> prioridades = new ArrayList<SigaIdDescr>();

//		final DpPessoaSelecao pessoaSel = new DpPessoaSelecao();
//		final DpLotacaoSelecao lotacaoSel = new DpLotacaoSelecao();
//
//		pessoaSel.buscarPorObjeto(pd.getPessoa());

		prioridades.add(new SigaIdDescr(WfPrioridade.MUITO_ALTA, "Muito Alta"));
		prioridades.add(new SigaIdDescr(WfPrioridade.ALTA, "Alta"));
		prioridades.add(new SigaIdDescr(WfPrioridade.MEDIA, "Média"));
		prioridades.add(new SigaIdDescr(WfPrioridade.BAIXA, "Baixa"));
		prioridades.add(new SigaIdDescr(WfPrioridade.MUITO_BAIXA, "Muito Baixa"));
		result.include("prioridades", prioridades);
		result.include("task", util.inicializarTaskVO(new WfTarefa(pi)));
		result.include("dot", util.getDot(new WfTarefa(pi)));
		result.include("movs", pi.getMovimentacoes());
		result.include("piId", pi.getId());
		result.include("pi", pi);
	}

	/**
	 * Verifica se as tarefas que estão designadas para o usuário estão associadas a
	 * documentos do SIGA-DOC (por intermédio da variável iniciada com o prefixo
	 * "doc_". Se estiver, chama o método addTask(). Este método executado quando a
	 * action "doc.action" for chamada.
	 */
	@Get
	@Path("/app/doc")
	public void doc(String sigla) throws Exception {
		Map<String, List<WfTaskVO>> mobilMap = new TreeMap<String, List<WfTaskVO>>();

		SortedSet<WfTarefa> taskInstances = Wf.getInstance().getBL().getTaskList(sigla);
		for (WfTarefa ti : taskInstances) {
			if (ti.getInstanciaDeProcedimento().getStatus() != ProcessInstanceStatus.PAUSED)
				continue;
			String principal = ti.getInstanciaDeProcedimento().getPrincipal();
			if (principal == null)
				continue;

			WfTaskVO task = new WfTaskVO(ti, sigla, so.getTitular(), so.getLotaTitular());
			if (!mobilMap.containsKey(principal)) {
				mobilMap.put(principal, new ArrayList<WfTaskVO>());
			}
			List<WfTaskVO> tasks = mobilMap.get(principal);
			tasks.add(task);
		}
		result.include("mobilMap", mobilMap);
	}

	/**
	 * 
	 * 
	 * /** Action que finaliza uma tarefa com as variaveis atribuidas. Os valores
	 * são atribuídos às variáveis da tarefa e depois a tarefa é transferida ao
	 * próximo passo. Se a próxima tarefa for para o mesmo ator, esta é exibida
	 * imediatamente.
	 * 
	 * @return
	 * @throws CsisException
	 */
	@Transacional
	@Post
	@Path("/app/procedimento/{piId}/continuar")
	public void continuar(Long piId, String[] fieldNames, StringQualquer[] fieldValues, Integer detourIndex,
			String sigla) throws Exception {
		String cadastrante = getTitular().getSigla() + "@" + getLotaTitular().getSiglaCompleta();

		WfProcedimento pi = loadTaskInstance(piId);

		WfDefinicaoDeTarefa td = pi.getCurrentTaskDefinition();

		// TODO Pegar automaticamente

		// WfBL.assertPodeTransferirDocumentosVinculados(new WfTarefa(pi), cadastrante);

		Map<String, Object> param = new HashMap<>();

		if (td.getVariable() != null) {
			for (int n = 0, c = 0; n < fieldNames.length; n++) {
				// Associa cada variavel com seu valore especifico
				for (WfDefinicaoDeVariavel variable : td.getVariable()) {
					if (fieldNames[n] == null)
						continue;
					if (variable.getIdentifier().equals(fieldNames[n])
							&& variable.getEditingKind() == VariableEditingKind.READ_WRITE) {
						Object value;
						if (variable.getIdentifier().startsWith("doc_")) {
							value = param(variable.getIdentifier() + "_expedienteSel.sigla");
						} else if (variable.getIdentifier().startsWith("pes_")) {
							value = param(variable.getIdentifier() + "_pessoaSel.sigla");
						} else if (variable.getIdentifier().startsWith("lot_")) {
							value = param(variable.getIdentifier() + "_lotacaoSel.sigla");
						} else if (variable.getIdentifier().startsWith("dt_")) {
							value = SigaCalendar.converteStringEmData(fieldValues[c].toString());
							c++;
						} else if (variable.getIdentifier().startsWith("sel_")) {
							value = fieldValues[c];
							c++;
						} else {
							value = fieldValues[c];
							c++;

						}

						if (value instanceof StringQualquer)
							value = ((StringQualquer) value).toString();

						// TODO: Verifica se as variáveis "required" foram preenchidas
						if (variable.isRequired() && (value == null
								|| (value instanceof String && (((String) value).trim().length() == 0)))) {
							throw new AplicacaoException("O campo " + variable.getTitle() + " deve ser preenchido");
						}

						param.put(fieldNames[n], value);
					}
				}
			}
		}

		Integer desvio = null;
		if (detourIndex != null && td.getDetour() != null && td.getDetour().size() > detourIndex) {
			desvio = detourIndex;
		}

		Wf.getInstance().getBL().prosseguir(pi.getEvent(), desvio, param, getTitular(), getLotaTitular(),
				getIdentidadeCadastrante());

		// Redireciona para a pagina escolhida quando o procedimento criar uma
		// variavel pre-definida
		//
//		String redirectTo = (String) taskInstance.getVariable(WF_REDIRECT_TO);
//		if (redirectTo != null) {
//			// taskInstance.deleteVariable(WF_REDIRECT_TO);
//			WfDao.getInstance().getSessao().flush();
//			taskInstance.getProcessInstance().getContextInstance().deleteVariable(WF_REDIRECT_TO);
//			WfDao.getInstance().getSessao().flush();
//			result.redirectTo(redirectTo);
//			return;
//		}

		// Apresenta a pagina do documento quando a sigla for fornecida
		//
		if (sigla != null) {
			result.redirectTo("/../sigaex/app/expediente/doc/exibir?sigla=" + sigla);
			return;
		}

		// Verificar se um novo task foi criado para o process instance em
		// questão. Se esse
		// task existir e for designado para o mesmo ator, então a próxima
		// página a ser exibida será a página de apresentação do task, e não a
		// página inicial.
		if (pi.getStatus() == ProcessInstanceStatus.PAUSED && pi.getLotacao() != null
				&& pi.getLotacao().equivale(getLotaTitular())) {
			result.redirectTo(this).procedimento(pi.getId());
			return;
		}

		result.redirectTo(this).procedimento(pi.getId());
	}

	@Transacional
	@Post
	@Path("/app/procedimento/{sigla}/pegar")
	public void pegar(String sigla, String siglaPrincipal) throws Exception {
		WfProcedimento pi = dao().consultarPorSigla(sigla, WfProcedimento.class);

		Wf.getInstance().getBL().pegar(pi, getTitular(), getLotaTitular(), getIdentidadeCadastrante());

		if (siglaPrincipal != null) {
			result.redirectTo("/../sigaex/app/expediente/doc/exibir?sigla=" + siglaPrincipal);
			return;
		}
		result.redirectTo(this).procedimento(pi.getId());
	}

	// TODO Pensar se queremos ter o conceito de conhecimento dentro do WF mesmo ou
	// se é melhor usar sempre o GC
	/**
	 * Grava o conhecimento a respeito de como uma tarefa deve ser executada.
	 * 
	 * @return
	 * @throws CsisException
	 */
//	public void saveKnowledge(Long tdId, String conhecimento) throws Exception {
//		TaskInstance taskInstance = getTaskInstance(tiId);
//
//		WfConhecimento cAntigo = ((WfDao) dao).consultarConhecimento(
//				taskInstance.getProcessInstance().getProcessDefinition().getName(), taskInstance.getName());
//
//		WfConhecimento cNovo = new WfConhecimento();
//		if (cAntigo != null) {
//			PropertyUtils.copyProperties(cNovo, cAntigo);
//			cNovo.setId(null);
//		} else {
//			cNovo.setProcedimento(taskInstance.getProcessInstance().getProcessDefinition().getName());
//			cNovo.setTarefa(taskInstance.getName());
//		}
//		cNovo.setDescricao(conhecimento);
//
//		dao.gravarComHistorico(cNovo, cAntigo, null, getIdentidadeCadastrante());
//
//		result.redirectTo(this).task(tiId);
//	}

	/**
	 * Action que finaliza uma tarefa com as variaveis atribuidas.
	 * 
	 * @return
	 * @throws CsisException
	 */
	@Transacional
	public void assignTask(Long piId, DpPessoaSelecao ator_pessoaSel, DpLotacaoSelecao lotaAtor_lotacaoSel,
			WfPrioridade prioridade, String justificativa) throws Exception {
		WfProcedimento pi = dao().consultar(piId, WfProcedimento.class, false);

		DpPessoa actor = ator_pessoaSel.getObjeto();
		DpLotacao lotaActor = lotaAtor_lotacaoSel.getObjeto();

		if (actor == null && lotaActor == null)
			throw new AplicacaoException(
					"Não é permitido designar a tarefa quando tanto a pessoa quanto a lotação estão em branco.");
		if (pi.getStatus() != ProcessInstanceStatus.PAUSED)
			throw new AplicacaoException("Não é permitido designar a tarefa quando o procedimento não está pausado.");

		boolean alterado = false;
		if ((pi.getPessoa() == null && actor != null)
				|| (pi.getPessoa() != null && (actor == null || !pi.getPessoa().equivale(actor)))) {
			pi.setPessoa(actor);
			alterado = true;
		}

		if ((pi.getLotacao() == null && lotaActor != null)
				|| (pi.getLotacao() != null && (lotaActor == null || !pi.getLotacao().equivale(lotaActor)))) {
			pi.setLotacao(lotaActor);
			alterado = true;
		}

		if (alterado) {
			WfBL.assertPodeTransferirDocumentosVinculados(new WfTarefa(pi), util.getSiglaTitular());
			WfBL.transferirDocumentosVinculados(pi, util.getSiglaTitular());
		}

		pi.setPrioridade(prioridade);

		// TODO Acrescentar uma movimentação de atribuição aqui!

		result.redirectTo(this).procedimento(pi.getId());
	}

	/**
	 * Insere um comentário à tarefa.
	 * 
	 * @return
	 * @throws CsisException
	 */
	@Transacional
	@Post
	@Path("/app/anotar")
	public void anotar(Long id, String descrMov) throws Exception {
		WfProcedimento pi = dao().consultar(id, WfProcedimento.class, false);
		Wf.getInstance().getBL().anotar(pi, descrMov, getTitular(), getLotaTitular(), getIdentidadeCadastrante());
		result.redirectTo(this).procedimento(id);
	}

	/**
	 * Insere um comentário à tarefa.
	 * 
	 * @return
	 * @throws CsisException
	 */
	@Transacional
	@Post
	@Path("/app/procedimento/{id}/anotacao/{idMov}/excluir")
	public void anotacaoExcluir(Long id, Long idMov) throws Exception {
		WfProcedimento pi = dao().consultar(id, WfProcedimento.class, false);
		for (WfMov mov : pi.getMovimentacoes()) {
			if (mov.getId().equals(idMov) && mov instanceof WfMovAnotacao) {
				Wf.getInstance().getBL().excluirAnotacao(pi, (WfMovAnotacao) mov);
				result.redirectTo(this).procedimento(id);
			}
		}
		throw new Exception("Movimentação não encontrada.");
	}

	/**
	 * Carrega as informações relativas à tarefa, tais como, a instância da tarefa,
	 * os logs, a prioridade e suas variáveis.
	 * 
	 * @throws Exception
	 */
	private WfProcedimento loadTaskInstance(Long piId) throws Exception {
		WfProcedimento pi = dao().consultar(piId, WfProcedimento.class, false);
		return pi;
	}
}