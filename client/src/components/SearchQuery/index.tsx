import { useState } from 'react';
import { CATEGORIES } from '../../constants/constants';
import { useSearchQuery } from './useSearchQuery';

export const SearchQuery = () => {
  const [searchKeyword, setSearchKeyword] = useState('');
  const [searchCategory, setSearchCategory] = useState(0);
  const { handleSearch } = useSearchQuery();

  const handleClick = () => {
    handleSearch(searchCategory, searchKeyword);
  };

  return (
    <section className="mt-6">
      <article className="relative mb-9">
        <input
          placeholder="검색어를 입력해 주세요."
          className="input bg-background-mobile p-2 pr-12"
          value={searchKeyword}
          onChange={(e) => setSearchKeyword(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') handleClick();
          }}
        />
        <button type="submit" className="absolute right-4 top-2" onClick={handleClick}>
          <svg
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
            strokeWidth={2.5}
            stroke="currentColor"
            className="w-6 h-6 text-main-brown">
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z"
            />
          </svg>
        </button>
      </article>
      <article className="flex flex-col space-y-2">
        <h2 className="text-lg font-bold">카테고리</h2>
        <div className="space-x-2 space-y-3">
          {CATEGORIES.map((category, idx) => (
            <button
              type="submit"
              className={`category-btn focus:bg-main-red focus:text-white active:bg-red-600 ${
                idx === searchCategory && 'bg-main-red'
              }`}
              key={category}
              onClick={() => setSearchCategory(idx)}>
              {category}
            </button>
          ))}
        </div>
      </article>
    </section>
  );
};
