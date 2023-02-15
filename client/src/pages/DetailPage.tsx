import { useLocation } from 'react-router-dom';
import { useQuery } from 'react-query';
import { CategoryBtn } from '../components/_common/CategoryBtn/CategoryBtn';
import { AuctionStatus } from '../components/DetailPage/AuctionStatus/AuctionStatus';
import { BidInformation } from '../components/DetailPage/BidInformation/BidInformation';
import { Chart } from '../components/DetailPage/Chart/Chart';
import { ImageList } from '../components/DetailPage/ImageList/ImageList';
import { TabBar } from '../components/_common/TabBar/TabBar';
import { SubHeader } from '../components/_common/Header/SubHeader/SubHeader';
import { CATEGORIES } from '../constants/constants';
import { productDetailAPI } from '../api/boardsAPI';

export const DetailPage = () => {
  const boardId = useLocation().pathname.split('/')[2];

  const { isLoading, error, data } = useQuery(
    'productDetail',
    async () => {
      const res = await productDetailAPI.get(boardId);
      return res.data;
    },
    {
      onError: (e) => console.error(e),
    }
  );

  // data가 undefined, null인 경우 TypeError 발생, 아닐 경우에만 분해되도록 함.
  const { image, categoryId, viewCount, finishedAt, statusId, description } = data || {};

  if (isLoading) return <div>Loading...</div>;
  if (error) return <div>에러가 발생하였습니다.</div>;

  return (
    <main className="base-layout">
      <SubHeader>상세 페이지</SubHeader>
      <section className="content-layout relative">
        <ImageList url={image} />
        <h1 className="text-xl font-semibold">아주 아름다운 모자 판매합니다.</h1>
        <CategoryBtn>{CATEGORIES[categoryId]}</CategoryBtn>
        <article className="flex justify-between px-2">
          <span>id : {boardId}</span>
          <span>조회수 : {viewCount}</span>
        </article>

        <AuctionStatus finishedAt={finishedAt} statusId={statusId} />

        <BidInformation />
        <Chart />
        <article>{description}</article>
      </section>
      <TabBar />
    </main>
  );
};
