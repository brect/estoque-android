package br.com.alura.estoque.repository;

import android.content.Context;

import java.util.List;

import br.com.alura.estoque.asynctask.BaseAsyncTask;
import br.com.alura.estoque.database.EstoqueDatabase;
import br.com.alura.estoque.database.dao.ProdutoDAO;
import br.com.alura.estoque.model.Produto;
import br.com.alura.estoque.retrofit.EstoqueRetrofit;
import br.com.alura.estoque.retrofit.callback.CallbackComRetorno;
import br.com.alura.estoque.retrofit.callback.CallbackSemRetorno;
import br.com.alura.estoque.retrofit.service.ProdutoService;
import retrofit2.Call;

public class ProdutoRepository {

    private final ProdutoDAO dao;
    private final ProdutoService service;

    public ProdutoRepository(Context context) {
        EstoqueDatabase db = EstoqueDatabase.getInstance(context);
        dao = db.getProdutoDAO();
        service = new EstoqueRetrofit().getProdutoService();
    }

    //carrega produtos internos e depois carrega produtos externos
    public void buscaProdutos(DadosCarregadosCallback<List<Produto>> callback) {
        buscaProdutosInternos(callback);
    }

    private void buscaProdutosInternos(DadosCarregadosCallback<List<Produto>> callback) {
        new BaseAsyncTask<>(dao::buscaTodos,
            resultado -> {
                //notifica que o dado esta ok
                callback.quandoSucesso(resultado);
                buscaProdutosNaAPI(callback);
            }).execute();
    }

    private void buscaProdutosNaAPI(DadosCarregadosCallback<List<Produto>> callback) {

        Call<List<Produto>> call = service.buscaTodos();
        call.enqueue(new CallbackComRetorno<>(
            new CallbackComRetorno.RespostaCallback<List<Produto>>() {
                @Override
                public void quandoSucesso(List<Produto> produtosNovos) {
                    atualizaInterno(produtosNovos, callback);
                }

                @Override
                public void quandoFalha(String erro) {
                    callback.quandoFalha(erro);
                }
            }
        ));
    }

    private void atualizaInterno(List<Produto> produtos,
                                 DadosCarregadosCallback<List<Produto>> callback) {
        new BaseAsyncTask<>(() -> {
            dao.salva(produtos);
           return (dao.buscaTodos());
        }, callback::quandoSucesso)
        .execute();
    }

    public void salva(Produto produto,
                      DadosCarregadosCallback<Produto> callback) {
        salvaNaAPI(produto, callback);

    }

    private void salvaNaAPI(Produto produto,
                            DadosCarregadosCallback<Produto> callback) {
        Call<Produto> call = service.salva(produto);
        call.enqueue(new CallbackComRetorno<>(
            new CallbackComRetorno.RespostaCallback<Produto>() {
                @Override
                public void quandoSucesso(Produto produtoSalvo) {
                    salvaInterno(produtoSalvo, callback);
                }

                @Override
                public void quandoFalha(String erro) {
                    callback.quandoFalha(erro);
                }
            }
        ));
    }

    private void salvaInterno(Produto produto,
                              DadosCarregadosCallback<Produto> callback) {
        new BaseAsyncTask<>(() -> {
            long id = dao.salva(produto);
            return dao.buscaProduto(id);
        }, callback::quandoSucesso)
                .execute();
    }

    public void edita(Produto produto,
                      DadosCarregadosCallback<Produto> callback) {
        editaNaAPI(produto, callback);
    }

    private void editaNaAPI(Produto produto,
                            DadosCarregadosCallback<Produto> callback) {
        Call<Produto> call = service.edita(produto.getId(), produto);
        call.enqueue(new CallbackComRetorno<>(
            new CallbackComRetorno.RespostaCallback<Produto>() {
                @Override
                public void quandoSucesso(Produto resultado) {
                    editaInterno(produto, callback);
                }

                @Override
                public void quandoFalha(String erro) {
                    callback.quandoFalha(erro);
                }
            }
        ));
    }

    private void editaInterno(Produto produto,
                              DadosCarregadosCallback<Produto> callback) {
        new BaseAsyncTask<>(() -> {
            dao.atualiza(produto);
            return produto;
        }, callback::quandoSucesso)
                .execute();
    }

    public void remove(Produto produto,
                       DadosCarregadosCallback<Void> callback) {
        removeNaAPI(produto, callback);
    }

    private void removeNaAPI(Produto produto,
                             DadosCarregadosCallback<Void> callback) {
        Call<Void> call = service.remove(produto.getId());
        call.enqueue(new CallbackSemRetorno(
                new CallbackSemRetorno.RespostaCallback() {
                    @Override
                    public void quandoSucesso() {
                        removeInterno(produto, callback);
                    }

                    @Override
                    public void quandoFalha(String erro) {
                        callback.quandoFalha(erro);
                    }
                }
        ));
    }

    private void removeInterno(Produto produto,
                               DadosCarregadosCallback<Void> callback) {
        new BaseAsyncTask<>(() -> {
            dao.remove(produto);
            return null;
        }, callback::quandoSucesso)
                .execute();
    }

    public interface DadosCarregadosCallback <T>{
        void quandoSucesso(T resultado);
        void quandoFalha(String erro);
    }
}
