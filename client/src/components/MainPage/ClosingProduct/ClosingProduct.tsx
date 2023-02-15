import { useQuery } from 'react-query';
import { Link } from 'react-router-dom';
import { ProductItemImg } from '../../_common/ProductItemImg/ProductItemImg';
import { closingProductAPI } from '../../../api/closingProductAPI';

export const ClosingProduct = () => {
  const { isLoading, error, data } = useQuery('closingProduct', () => closingProductAPI.get(), {
    // 현재 데이터가 없어서 쿼리를 1초단위로 보내는만큼 매초 에러가 발생해서 우선 주석처리 해두었습니다.
    // refetchOnMount: true,
    // refetchOnWindowFocus: true,
    // refetchInterval: 1000,
  });

  return (
    <div className=" w-full my-4 px-2">
      <div className="flex items-center">
        <h1 className="text-lg m-2 mr-1 font-bold">마감 임박</h1>
        <svg
          xmlns="http://www.w3.org/2000/svg"
          fill="none"
          viewBox="0 0 24 24"
          strokeWidth={1.5}
          stroke="red"
          className="w-5 h-5">
          <path strokeLinecap="round" strokeLinejoin="round" d="M12 6v6h4.5m4.5 0a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
      </div>
      <div className="flex gap-2 overflow-x-auto scrollbar-hide w-full">
        {isLoading && <div>Loading...</div>}
        {error && <div>마감 임박 상품이 없습니다.</div>}
        {data?.map((el) => {
          return (
            <Link key={el.boardId} to={`/detail/${el.boardId}`}>
              <div className="flex flex-col ml-2 min-w-[120px] w-[120px]">
                <ProductItemImg thumbnail={el.thumbnail} statusId={el.statusId} />
                <p className="text-xs line-clamp-1">{el.title}</p>
                <p className="text-base text-main-orange">{el.currentPrice} coin</p>
              </div>
            </Link>
          );
        })}
      </div>
    </div>
  );
};
