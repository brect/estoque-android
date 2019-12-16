package br.com.alura.estoque.repository;

import android.os.AsyncTask;

import java.io.IOException;
import java.util.List;

import br.com.alura.estoque.asynctask.BaseAsyncTask;
import br.com.alura.estoque.database.dao.ProdutoDAO;
import br.com.alura.estoque.model.Produto;
import br.com.alura.estoque.retrofit.EstoqueRetrofit;
import br.com.alura.estoque.retrofit.service.ProdutoService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.internal.EverythingIsNonNull;

public class ProdutoRepository {

    private final ProdutoDAO dao;
    private final ProdutoService service;

    public ProdutoRepository(ProdutoDAO dao) {
        this.dao = dao;
        service = new EstoqueRetrofit().getProdutoService();
    }

    //carrega produtos internos e depois carrega produtos externos
    public void buscaProdutos(DadosCarregadosListener<List<Produto>> listener) {
        buscaProdutosInternos(listener);
    }

    private void buscaProdutosInternos(DadosCarregadosListener<List<Produto>> listener) {
        new BaseAsyncTask<>(dao::buscaTodos,
                resultado -> {
                    //notifica que o dado esta ok
                    listener.quandoCarregados(resultado);
                    buscaProdutosNaAPI(listener);
                }).execute();
    }

    private void buscaProdutosNaAPI(DadosCarregadosListener<List<Produto>> listener) {

        Call<List<Produto>> call = service.buscaTodos();
        //notifica que o dado esta ok
        new BaseAsyncTask<>(() -> {
            try {
                Response<List<Produto>> resposta = call.execute();
                List<Produto> produtosNovos = resposta.body();
                dao.salva(produtosNovos);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return dao.buscaTodos();
        }, listener::quandoCarregados)
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void salva(Produto produto,
                      DadosCarregadosCallback<Produto> callback) {
        salvaNaAPI(produto, callback);

    }

    private void salvaNaAPI(Produto produto,
                            DadosCarregadosCallback<Produto> callback) {
        Call<Produto> call = service.salva(produto);
        call.enqueue(new Callback<Produto>() {
            @Override
            @EverythingIsNonNull
            public void onResponse(Call<Produto> call,
                                   Response<Produto> response) {
                //verifica se resposta foi ok
                if (response.isSuccessful()){
                        Produto produtoSalvo = response.body();
                        //notificar que o dado esta ok
                    //verifica se corpo da requisicao eh nulo
                    if (produtoSalvo != null){
                        salvaInterno(produtoSalvo, callback);
                    }
                }else {
                    //notifica uma falha na requisicao
                    callback.quandoFalha("Resposta não sucedida");
                }
            }

            @Override
            @EverythingIsNonNull
            public void onFailure(Call<Produto> call, Throwable t) {
                //notifica uma falha
                callback.quandoFalha("Falha de comunicao: " + t.getMessage());
            }
        });
    }

    private void salvaInterno(Produto produto,
                              DadosCarregadosCallback<Produto> callback) {
        new BaseAsyncTask<>(() -> {
            long id = dao.salva(produto);
            return dao.buscaProduto(id);
        }, callback::quandoSucesso)
                .execute();
    }

    public interface DadosCarregadosListener <T>{
        void quandoCarregados(T resultado);
    }

    public interface DadosCarregadosCallback <T>{
        void quandoSucesso(T resultado);
        void quandoFalha(String erro);
    }
}
